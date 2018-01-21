/*  Luqhasal
*   Operating Systems
*/

import java.io.BufferedReader;
import java.io.FileReader;

public class Memsim {
    public static void main(String[] args) {
        int page_offset = 12;			// page is 2^12 = 4KB
	int frames;
	boolean debug = false;

        BufferedReader input = null;
        MMU mmu = null;
        
        /* read parameters */
        //the file
        try {
           input = new BufferedReader(new FileReader(args[0]));
        }
        catch (java.io.FileNotFoundException e) {
            System.out.println("Input '" + args[0] + "' could not be found");
            System.out.println("Usage: java Memsim inputfile numberframes replacementmode debugmode");
            System.exit(-1);
        }
        
        //number of frames
        frames = Integer.parseInt(args[1]);
	if (frames < 1) {
            System.out.println("Frame number must be at least 1");
            System.exit(-1);
        }
        
        //the replacement mode
        if (args[2].equals("rand"))
            mmu = new RandMMU(frames);
        else if (args[2].equals("fifo"))
            mmu = new fifoMMU(frames);
        else if (args[2].equals("lru"))
            mmu = new LruMMU(frames);
        else if (args[2].equals("esc"))
            mmu = new EscMMU(frames);
        else {
            System.out.println("Usage: java Memsim inputfile numberframes replacementmode debugmode");
            System.out.println("replacementmodes are [ rand | fifo | lru | esc ]");
            System.exit(-1);
        }
        
        //debug mode?
        if (args[3].equals("debug"))
            debug = true;
        else if (args[3].equals("quiet"))
            debug = false;
        else {
            System.out.println("Usage: java Memsim inputfile numberframes replacementmode debugmode");
            System.out.println("debugmode are [ debug | quiet ]");
            System.exit(-1);
        }
        
        /* Process the traces from the file */
        String traceLine;
        String[] traceCmd;
        long logical_address;
        int page_number, frame_number;
	int victim_page;
	boolean is_modified;
	int allocated = 0;
        int no_events = 0;
	int disk_writes = 0, disk_reads = 0;
        
        try {
            traceLine = input.readLine();
            while (traceLine != null) {
                traceCmd = traceLine.split(" ");
                
                //convert from hexadecimal address from file, to appropriate page number
                logical_address = Long.parseLong(traceCmd[0],16);
                page_number = (int) (logical_address >>> page_offset);
		frame_number = mmu.checkInMemory(page_number);
                
		if ( frame_number == -1) {
                // page fault 
		disk_reads++;
		if (debug) 
                    System.out.println("Page fault   " + page_number);
		if (allocated < frames ) {
			mmu.allocateFrame(page_number);
			allocated++;
		}
		else { 
			
			victim_page = mmu.selectVictim(page_number);
			is_modified = mmu.lastVictimStatus( );
		        if (is_modified){
				disk_writes++;
				if (debug) System.out.println("Disk write  " + victim_page);
			}
			else  if (debug) System.out.println("Discard      " + victim_page);
		}
		}

                //process read or write
                if (traceCmd[1].equals("R")){
                    mmu.readMemory(page_number);
		    if (debug) System.out.println("reading      " + page_number);
		}
                else if (traceCmd[1].equals("W")){
                    mmu.writeMemory(page_number);
		    if (debug) System.out.println("writting     " + page_number);
		}
                else {
                    System.out.println("Badly formatted file. Error on line " + (no_events+1));
                    System.exit(-1);
                }
                
                no_events++;
                traceLine = input.readLine();
            }
        }
        catch (java.io.IOException e) {
            System.out.println("Error reading input file");
            System.exit(-1);
        }
		catch (NumberFormatException e) {
			System.out.println("Memory address strange on line " + (no_events+1));
            System.exit(-1);
		}
        
        /* Print results */
        System.out.println("total memory frames:  " + frames);
        System.out.println("events in trace:      " + no_events);
        System.out.println("total disk reads:     " + disk_reads );
        System.out.println("total disk writes:    " + disk_writes);
	java.text.DecimalFormat f = new java.text.DecimalFormat("0.0000");
	System.out.println("page fault rate: " + f.format(((double)disk_reads)/no_events));
    }
    
}
