/*  Luqhasal
*   Operating Systems
*/

/**
* MMU using least recently used replacement strategy
* Need to keep track of reference order 
*/

public class LruMMU implements MMU {

    private final int frameCount;                     // number of frame
    private int victimPointer;                    // page table pointer 
    private int victimFrame;                        // page table pointer entry
    private final pageEntry[] pageTable;              // page table array
    private int emptyFramePointer;                      // pointer assigning
    private int localSwitch;                            // pointer assigning
    private int laststatusPointer;                        // pointer assigning
    private int laststatusFrame;  
    private int laststatusPage;
    private String laststatusProcess;
    private boolean laststatusModified;

    /* each page has a resident and a modified bit
     */
    public LruMMU(int frames) {
        //System.out.println("========= LruMMU");

        // variables
        victimPointer = -1;
        frameCount = frames;
        //System.out.println("=========frameCount " + frameCount);    
        emptyFramePointer = -1;  // -1 is empty bit
        localSwitch = 0;
        victimFrame = 0;
        laststatusPointer = 0;
        laststatusFrame = 0;
        laststatusPage = 0;
        laststatusProcess = "";
        laststatusModified = false;
        
        // initialize page table array and add object reference
        pageTable = new pageEntry[frameCount];
        for (int i = 0; i < pageTable.length; i++) { pageTable[i] = new pageEntry();}
    }
    
    /* this method updates the reference status of a resident page
     */
    public void readMemory(int page_number) {
        //System.out.println("========= readMemory");

        // find the page table for page number then set its process bit to R
        for (int i = 0; i < pageTable.length; i++) {
            if (page_number == pageTable[i].getpageNum()) {
                pageTable[i].setprocessBit("R");
                pageTable[i].setageBit(99);
            }
        }
    }
    
    /* this method updates the reference status of a resident page and
     * records it has been modified
     */
    public void writeMemory(int page_number) {
        //System.out.println("========= writeMemory");

        // find the page table for page number then set its process bit to W and modified bit to true
        for (int i = 0; i < pageTable.length; i++) {
            if (page_number == pageTable[i].getpageNum()) {
                pageTable[i].setprocessBit("W");
                pageTable[i].setmodifiedBit(true);
                pageTable[i].setageBit(99);
            }
        }
    }

    /* check if a page is resident
     * returns its location (frame number) or -1 if not resident
     */
    public int checkInMemory(int page_number) {
        //System.out.println("========= checkInMemory");
        //System.out.println("========= Need to insert Page number = " + page_number);

        int index = -1;     //-1 is empty bit

         // decrement all pages by 1 for every turn
        for (int i = 0; i < pageTable.length; i++) {
            if (-1 != pageTable[i].getpageNum()) {
                pageTable[i].decrementageBit();
            }
        }       

        // check page table if the page number is recorded
        for (int i = 0; i < pageTable.length; i++) {
            if (page_number == pageTable[i].getpageNum()) {
                index = pageTable[i].getpagetoFrame();
            }
        }
            return index;
    }
    
    /* it allocate a page into a free frame
     */
    public void allocateFrame(int page_number) {
        //System.out.println("========= allocateFrame");
        //System.out.println("========= Need to insert Page number = " + page_number);
    
        // iterate page table for empty frame to insert and get empty pointer in order
        for (int i = 0; i < pageTable.length; i++) {
            if (-1 == pageTable[i].getpagetoFrame()) {
                emptyFramePointer = i;
                //System.out.println("========= use empty frame = " + emptyFramePointer);
                break;
            }
        }

        // switching different pointer assigning - initial 0 for first page faults
        switch (localSwitch) {
            
                    // update the page table and assign new pagetoframe - first fault pointer
            case 0: pageTable[emptyFramePointer].setpageNum(page_number);
                    pageTable[emptyFramePointer].setpagetoFrame(emptyFramePointer);
                    pageTable[emptyFramePointer].setlocalPtr(emptyFramePointer);
                    //System.out.println("========= case 0 and set to frame " + pageTable[emptyFramePointer].getpagetoFrame());
                    break;
            
                    // update the page table and assign new pagetoframe - victim pointer
            case 1: pageTable[victimPointer].setpageNum(page_number);
                    pageTable[victimPointer].setpagetoFrame(victimFrame);
                    pageTable[victimPointer].setlocalPtr(victimPointer);
                    pageTable[victimPointer].setmodifiedBit(false);
                    pageTable[victimPointer].setprocessBit("");
                    pageTable[victimPointer].setageBit(99);
                    //System.out.println("========= case 1 and set to frame " + pageTable[victimPointer].getpagetoFrame());
                    break;
        }    
    }
    
    /* it select the victim, allocates the page into the selected frame 
     * and returns the number of the page replaced
     */
    public int selectVictim(int page_number) {
        //System.out.println("========= selectVictim");
        //System.out.println("========= Need to insert Page number = " + page_number);

        victimPointer = 0;
        //System.out.println("========= victimPointer = " + victimPointer);

        int index = -1;     //-1 is empty bit
        localSwitch = 1;    //victim pointer activated        

        // use victimPointer to get the page number to return index and its frame number stored entry
        // find pointer to point the least request page page (minValue)
        int minValue = 99;
        //System.out.println("========= BEFORE age = " + minValue + " / " + pageTable[pageTable.length-1].getpageNum());   
        for (int i = 0; i < pageTable.length; i++) {
            //System.out.println("========= LOOP age = " + pageTable[i].getageBit() + " / " + pageTable[i].getpageNum());
            if (pageTable[i].getageBit() < minValue) {
                minValue = pageTable[i].getageBit();
                victimPointer = i;      // victim pointer with the minimum value
                //System.out.println("========= Take out page with age  = " + minValue + " / " + pageTable[victimPointer].getpageNum() + " - status " + pageTable[victimPointer].getprocessBit());
                }
        }  

        index = pageTable[victimPointer].getpageNum();
        victimFrame = pageTable[victimPointer].getpagetoFrame();
        //System.out.println("========= victimPointer = " + victimPointer + " / victimFrame = " + victimFrame + " / victimPage = " + pageTable[victimPointer].getpageNum() + " / victimProcess = " + pageTable[victimPointer].getprocessBit());

        // assign the old values temporarily
        laststatusPointer = victimPointer;
        laststatusFrame = victimFrame;
        laststatusPage = pageTable[victimPointer].getpageNum();
        laststatusProcess = pageTable[victimPointer].getprocessBit();
        laststatusModified = pageTable[victimPointer].getmodifiedBit();

        allocateFrame(page_number);

        return index;
    }
    
    /* it returns true if the last victim was a modified page
     * false otherwise
     */
    public boolean lastVictimStatus( ) {
        //System.out.println("========= lastVictimStatus");
        //System.out.println("========= laststatusvictimPointer = " + laststatusPointer + " / victimFrame = " + laststatusFrame + " / victimPage = " + laststatusPage + " / " + laststatusProcess + " / " + laststatusModified);
        
        boolean index = false;      // initial boolean

        // check boolean if modified index is true otherwise false not modified
        // if modifiedBit = true then disk_writes to hard disk 'coz already written
        // if modifiedBit = false then just discard it and replace 
        if (laststatusModified){
            index = true;
        }

        return index;
    }

    /* custom-defined page
     */
    class pageEntry {

        private int pageNum;
        private int pagetoFrame;
        private boolean modifiedBit;
        private String processBit;
        private int ageBit;
        private int localptr;

        public pageEntry() {
            pageNum = -1;       //-1 is empty bit
            pagetoFrame = -1;   //-1 is empty bit
            modifiedBit = false;
            processBit = "";
            ageBit = 99;        //-1 is initial age
            localptr = -1;             
        }
        
        public int getpageNum() {
            return pageNum;
        }
            
        public int getpagetoFrame() {
            return pagetoFrame;
        }             
        
        public boolean getmodifiedBit() {
            return modifiedBit;
        }         

        public String getprocessBit() {
            return processBit;
        }

        public void decrementageBit() {
            ageBit--;
        }                

        public int getageBit() {
            return ageBit;
        }

        public int getlocalPtr() {
            return localptr;
        }        

        public void setpageNum(int pageNum) {
            this.pageNum = pageNum;
        }
            
        public void setpagetoFrame(int pagetoFrame) {
            this.pagetoFrame = pagetoFrame;
        }            
            
        public void setmodifiedBit(boolean modifiedBit) {
            this.modifiedBit = modifiedBit;
        }
            
        public void setprocessBit(String processBit) {
            this.processBit = processBit;
        }

        public void setageBit(int ageBit) {
            this.ageBit = ageBit;
        }

        public void setlocalPtr(int localptr) {
            this.localptr = localptr;
        }               
    }

}