package spy.g3;

import java.util.List;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue; 

import spy.sim.Point;
import spy.sim.Record;
import spy.sim.CellStatus;
import spy.sim.Simulator;
import spy.sim.Observation;
import java.util.AbstractMap;


public class Player implements spy.sim.Player {

    private class Entry implements Comparable<Entry> {
    public Double key;
    public Point p;

    public Entry(Double key, Point pt) {
        this.key = key;
        this.p = pt;
    }

    // getters

    @Override
    public int compareTo(Entry other) {
        return this.key.compareTo(other.key);
    }
}
    
    private ArrayList<ArrayList<Record>> records; // 2-dim list of cells on map (personal records)
    private int id;
    private Point loc; // Current location on map
    private Boolean _target;  // whether target has been located
    private Boolean _package; //whether package has been located
    private Point package_Location; // package location
    private Point target_Location; // target location
    private int[][] grid; // status of cells: -2 id water, -1 is muddy, 0 is normal, 1 is target & Package
    private int[][] visited; // whether the cell has been visited
    private int[][] explored; // whether the cell has been explored i.e. neighbors searched, in the current iteration
    private List<Point> proposedPath; // proposed safe path from package to target
    private Boolean found_path = false; // whether a safe proposed path has been found
    private HashMap<Point,Integer> trap_count; // maintains frequency of visiting a location
    private Integer time; // keeps track of elapsed time
    private Point unexplored; // the next unexplored point to visit

    private ArrayList<Point> wayPoints;

    private ArrayList<ArrayList<Record>> landInfo; // similar to 'records' but global for dry land claims
    private ArrayList<ArrayList<Record>> mudInfo; // similar to 'records' but global for muddy land claims
    
    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        // Initialize parameters

	    this._package=false;
        this._target = false;
        this.grid = new int[100][100];
        this.visited = new int[100][100];
        this.explored = new int[100][100];
        this.package_Location = new Point(-1,-1);
        this.target_Location = new Point(-1,-1);
        this.proposedPath = new ArrayList<Point>();
        this.trap_count =  new HashMap<Point,Integer>();
        this.time =0;
        this.unexplored = getRandomUnexplored();

        this.wayPoints = new ArrayList<Point>();

        // set status of water cells and set unknown cells to muddy

        for(int i=0;i<100;i++)
        {
            for(int j=0;j<100;j++)
            {
                grid[i][j] = -1;
                visited[i][j] = 0;
            }
        }

        for(int i=0;i<waterCells.size();i++)
        {
            Point tmp = waterCells.get(i);
            visited[tmp.x][tmp.y]= -2;
            grid[tmp.x][tmp.y] = -2;
        }
	
        // create records for sending and bookeeping
        this.id = id;
        this.records = new ArrayList<ArrayList<Record>>();
        for (int i = 0; i < 100; i++)
        {
            ArrayList<Record> row = new ArrayList<Record>();
            for (int j = 0; j < 100; j++)
            {
                row.add(null);
            }
	    // System.out.println(row);
            this.records.add(row);
        }
    }
    
    //Observes the vicinity, upfates grid & visited for all visible cells
    // Adds observations to record
    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {
        this.loc = loc;
        visited[loc.x][loc.y] = 1;
	// System.out.println("Called observe function =========");
        for (Map.Entry<Point, CellStatus> entry : statuses.entrySet())
        {
            Point p = entry.getKey();
            CellStatus status = entry.getValue();
            Record record = records.get(p.x).get(p.y);

            if(status.getC()==0)
                {
                    grid[p.x][p.y] = 0;
                    visited[p.x][p.y] = 1;
                }
            else if(status.getC()==1)
                {
                    grid[p.x][p.y] = -1;
                    visited[p.x][p.y] = 1;
                }

            if(status.getPT()==1)
            {
                grid[p.x][p.y] = 1;
                package_Location.x = p.x;
                package_Location.y = p.y;
                _package =true;
            }
            else if (status.getPT()==2)
            {
                grid[p.x][p.y] = 1;
                target_Location.x = p.x;
                target_Location.y = p.y;
                _target =true;
            }
	        // System.out.println(p + " " + status + " " );
            if (record == null || record.getC() != status.getC() || record.getPT() != status.getPT())
            {
                ArrayList<Observation> observations = new ArrayList<Observation>();
                record = new Record(p, status.getC(), status.getPT(), observations);
                records.get(p.x).set(p.y, record);
            }
            record.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));
        }
    }
    
    //Sends records when demanded
    public List<Record> sendRecords(int id)
    {
        // System.out.println("Called sendRecords ======");	  
        ArrayList<Record> toSend = new ArrayList<Record>();
        if(time%50==0)
        for (ArrayList<Record> row : records)
        {
            for (Record record : row)
            {
                if (record != null)
                {
                    toSend.add(record);
                }
            }
        }
        return toSend;
    }
    
    // receives records and updates grid &  visited according ro info provided.
    // NOTE:  Right now all information is assumed to be true. We are trusting other players on blind faith.
    // Will have to change in presence of spy
    public void receiveRecords(int id, List<Record> records)
    {
	   // System.out.println("Called receiveRecords Command ========");
       for(int i=0;i<records.size();i++)
       {
         Record new_record = records.get(i);
         Point p = new_record.getLoc();
         Record curr_record = this.records.get(p.x).get(p.y);

         visited[p.x][p.y] = 1;  // to be changed in case of spy

         if(new_record.getC()==0)
            {
                grid[p.x][p.y] = 0;    
            }
         else if(new_record.getC()==1)
            {
                grid[p.x][p.y] = -1;
            }

            if(new_record.getPT()==1)
            {
                grid[p.x][p.y] = 1;
                package_Location.x = p.x;
                package_Location.y = p.y;
                _package =true;
            }
            else if (new_record.getPT()==2)
            {
                grid[p.x][p.y] = 2;
                target_Location.x = p.x;
                target_Location.y = p.y;
                _target =true;
            }

          if(curr_record==null)
          {
            curr_record = new Record(new_record);
            this.records.get(p.x).set(p.y, curr_record);

            } 

           else
            curr_record.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));

       }

    }
    
    //Proposes the path if on package location
    public List<Point> proposePath()
    {
        if(proposedPath.size()>1)
            return proposedPath;
        return null;
    }
    
    // Vote for proposed paths
    //NOTE: Currently trusting all paths proposed by the players. Assuming the correctness of their implementations
    public List<Integer> getVotes(HashMap<Integer, List<Point>> paths)
    {
        for (Map.Entry<Integer, List<Point>> entry : paths.entrySet())
        {
            ArrayList<Integer> toReturn = new ArrayList<Integer>();
            toReturn.add(entry.getKey());
            return toReturn;
        }
        return null;
    }
    
    // No idea what this is for
    public void receiveResults(HashMap<Integer, Integer> results)
    {
       	// System.out.println("Called receiveResults Command =======");
    }

    private void setWayPoints()
    {
        Point wp1 = new Point(0,99);
        Point wp2 = new Point(99,99);
        Point wp3 = new Point(99,0);
        Point wp4 = new Point(0,0);
        Point wp5 = new Point(50,50);

        Point wp6 = new Point(50,82);
        Point wp7 = new Point(50,17);
        Point wp8 = new Point(82,50);
        Point wp9 = new Point(17,50);

        wayPoints.add(wp1);
        wayPoints.add(wp2);
        wayPoints.add(wp3);
        wayPoints.add(wp4);
        wayPoints.add(wp5);
        wayPoints.add(wp6);
        wayPoints.add(wp7);
        wayPoints.add(wp8);


    }



    // Gets the nearest unvisited cell. This is done inorder to explore new areas with minimal repetition
    private Point getNearestUnExplored(Point curr)
    {

        double min_dist = Integer.MAX_VALUE;
        Point next_move = new Point(-2000,-2000);

        for(int i=0;i<100;i++)
        {
            for(int j=0;j<100;j++)
            {
                if(grid[i][j]==-2 || visited[i][j]==1) continue;

                double dist_curr = Math.abs(curr.x-i) + Math.abs(curr.y-j) - grid[i][j];

                if(dist_curr<min_dist)
                {
                    min_dist = dist_curr;
                    next_move = new Point(i,j);
                }
            }
        }
        return next_move;

    }

    //Move to a random unvisited cell if trapped. Intended as tie breaker
    private Point getRandomUnexplored()
    {
        Random rand = new Random();
        int n = rand.nextInt(50);

        for(int i=n;i<100;i++)
        {
            for(int j=0;j<100;j++)
            {
                if(grid[i][j]==-2 || visited[i][j]==1) continue;

                return new Point(i,j);
            }
        }

        return new Point(-1000,-1000);
    }


    //Dijkstra'a shortest path algorithm to find shortest path from loc to destination.
    // if safe set to true it finds the shortest path from normal cells only
    // if sade set to false, it finds rge shortest path overall including muddy cells 
    private Point getNextOnPath(Point loc,Point destination,Boolean safe)
    {
        HashMap<Point, Double> dist = new HashMap<Point, Double>();
        HashMap<Point, Point> parent = new HashMap<Point, Point>();

        Boolean found = false;

        //
    // reset exploration matrix before searching for next exploration site
    //
        for(int i=0;i<100;i++)
        {
            for(int j=0;j<100;j++)
                explored[i][j] = 0;
        }

        for(int i=0;i<100;i++)
        {
            for(int j=0;j<100;j++)
            {
                dist.put(new Point(i,j),Double.POSITIVE_INFINITY);
            }
        }

        dist.put(loc,0.0);

        PriorityQueue<Entry> q = new PriorityQueue<>();
        Entry s = new Entry(0.0,loc);
        q.add(s);

        
        while(q.peek()!=null && !found)
            {
                Entry tmp = q.poll();
                Point next = tmp.p;
                explored[next.x][next.y]=1;
                for(int i = next.x-1;i<=next.x+1;i++)
                {
                    for(int j = next.y-1;j<=next.y+1;j++)
                    {
                        double diff = Math.abs(next.x-i) + Math.abs(next.y-j);
                        Double val = Double.POSITIVE_INFINITY; 

                        if(i < 0 || i>=100 || j<0 || j>=100 || explored[i][j]==1 || grid[i][j]==-2) continue;
                        if(safe && grid[i][j]<0) continue;

                        if(diff>1)
                            val = tmp.key + 1.5 - 2*grid[i][j];
                        else
                            val = tmp.key + 1 - 2*grid[i][j];

                        Point pt = new Point(i,j);
                        Double distance = dist.get(pt);


                        if(val<distance)
                        {
                            dist.put(pt,val);
                            parent.put(pt,next);
                            Entry new_entry = new Entry(val,pt);
                            q.add(new_entry);
                        }

                        if(destination.x == i && destination.y ==j)
                           {
                            found =true;
                            System.out.println("location is  " + loc + " destination is " + destination);
                            System.out.println("found the destination at distance "  + val);
                        }

                             
                    }

                }
            }


            Point next = new Point(destination);
            Point prev = new Point(-1000,-1000);

            if(!found_path)
            proposedPath.clear();

            while(parent.get(next)!=null)
            {
                System.out.println(next);
                prev = new Point(next.x,next.y);
                if(!found_path)
                proposedPath.add(0,new Point(prev.x,prev.y));
                next = new Point(parent.get(next));
                
            }

            System.out.println("next move point is "  + prev);
            return prev;


    }


    //Computes the next move    
    public Point getMove()
    {

    time++;
    

    visited[loc.x][loc.y] = 1; //mark current location as visited
	// System.out.println("Called getMove Command =======");
    Point move = new Point(-1000,-1000);

    //
    // If target and package have been located, try to find a safe path between them. If found set found_path to true
    //
    if(_target && _package)
    {
        //wait
        if(!found_path)
        {
            proposedPath.clear();
            
            Point start = package_Location;
            getNextOnPath(start,target_Location,true);

            proposedPath.add(0,start);

            Point reach_pt = proposedPath.get(proposedPath.size()-1);

            if(reach_pt.x==target_Location.x && reach_pt.y == target_Location.y)
                found_path = true;
            
            for(int i=0;i<proposedPath.size();i++)
            {
                System.out.println(proposedPath.get(i));
            }
        }
        //announce shortest path
    }

    //
    // if a safe path has been found, proceed to the package on the shortest path from current location
    //
    if(_target && _package && found_path && (loc.x!=package_Location.x || loc.y!=package_Location.y))
    {
        //go to package
        Point next = getNextOnPath(loc,package_Location,false);
        move = next;
        System.out.println("location is " + loc + " moving to " + move );
        int x  = move.x - loc.x;
        int y = move.y - loc.y;

        
        return new Point(x,y);
    }
    //
    //If safe path has been found from package to target and you are package location, then wait and announce proposed path.
    //Also vote for appropriate paths
    //
    else if(_target && _package && found_path)
    {
        return new Point(0,0);
    }
    //
    // If you are enroute to the next unexplored cell and haven't reached it then continue along the shortest path to that cell
    //
    if(unexplored.x>=0 && unexplored.y>=0 && visited[unexplored.x][unexplored.y]!=1)
    {
        Point next = getNextOnPath(loc,unexplored,false);
            move = next;
            int x  = move.x - loc.x;
            int y = move.y - loc.y;
            // System.out.println("moving to closest unexplored from " + loc + " moving to " + unexplored + "via "  + move );
            // System.out.println("the cell condition for " + move +   " is  " + grid[move.x][move.y] );

            if(x>=-1 && y>=-1)
            return new Point(x,y);
    }
    //
    //If you have reached the last unexplored cell, then find the next nearest unexplored cell. Set unexplored to next site
    //
    Point next_loc = getNearestUnExplored(loc);
    unexplored = next_loc;

    //
    //get next move for new unexplored site
    //
    
    Point next = getNextOnPath(loc,next_loc,false);

    //
    // maintain a trap count for current location
    //
    if(trap_count.containsKey(next))
    {
        trap_count.put(next,trap_count.get(next)+1);
    }
    else
    {
        trap_count.put(next,0);
    }
    //
    //if you have visited the same site more than 10 times, then probably trapped. Select a random unexplored cell and proceed towards that to break free.
    //
    if(trap_count.get(next)<10)
    {
        move = next;
        int x  = move.x - loc.x;
        int y = move.y - loc.y;
         if(x>=-1 && y>=-1)
            return new Point(x,y);
    }
    else
    {
        unexplored = getRandomUnexplored();
        move = getNextOnPath(loc,unexplored,false);
            int x  = move.x - loc.x;
            int y = move.y - loc.y;

            if(x>=-1 && y>=-1)
            return new Point(x,y);

    }

        //
        //This basically should never happem. It implies that you visited all possible cells and still found no valid path!
        //
        return move;
    }
}
