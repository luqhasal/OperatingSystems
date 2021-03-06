/*  Luqhasal
*   Operating Systems
*/

/**
* MMU using enchanced second chance replacement strategy
* Page replacement based on the R and M bits
*/
import java.util.Collections;
import java.util.Arrays;

public class EscMMU implements MMU {

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
    private int laststatusModified;
    private int handPointer;

    /* each page has a resident and a modified bit
     */
    public EscMMU(int frames) {
        //System.out.println("========= EscMMU");

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
        laststatusModified = 0;
        handPointer = 0;
        
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
                pageTable[i].setreferenceBit(1);
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
                pageTable[i].setmodifiedBit(1);
                pageTable[i].setreferenceBit(1);
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
                    pageTable[victimPointer].setmodifiedBit(0);
                    pageTable[victimPointer].setreferenceBit(0);
                    pageTable[victimPointer].setprocessBit("");
                    //System.out.println("========= case 1 and set to frame " + pageTable[victimPointer].getpagetoFrame());
                    break;
        }    
    }
    
    /* it select the victim, allocates the page into the selected frame 
     * and returns the number of the page replaced
     */
    public int selectVictim(int page_number) {

        int index = -1;     //-1 is empty bit

        // return the frame position with lowest class nonempty
        int a = anchor();

        // iterate the hand pointer until it reach the intended frame position
        //victimPointer = clockMovement(a);
        clockMovement(a);

        //System.out.println("========= selectVictim");
        //System.out.println("========= Need to insert Page number = " + page_number);
        //System.out.println("========= victimPointer " + victimPointer);
        
        localSwitch = 1;    //victim pointer activated

        // use victimPointer to get the page number to return index and its frame number stored entry
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

    // move one step at a time
    public void clockMovement(int a) {
        //System.out.println("========= clockMovement");
        //System.out.println("========= handPointer " + handPointer);
        //System.out.println("========= page number " + pageTable[handPointer].getpageNum());

        int anchor = a;
        int anchorClass = pageTable[anchor].getClassRM();
        //System.out.println("========= anchor " + anchor);
        //System.out.println("========= classRM anchor " + pageTable[anchor].getClassRM());

        // iterate down the list
        while (true) {

            //System.out.println("========= after while - handpointer " + handPointer);

            // compare
            if (handPointer == anchor) {
                //System.out.println("========= compare - page number " + pageTable[handPointer].getpageNum());
                //System.out.println("========= victimPointer iteration found " + handPointer);
                break;
            }            

            //increment victimPointer 0 to frameCount if hit bottom then reset to 0
            if (handPointer == frameCount) {
                handPointer = 0;
                //System.out.println("========= reset to the top array pointer - page number " + pageTable[handPointer].getpageNum());
                //System.out.println("========= victimPointer A = " + victimPointer);
            } else {
                int previouspointer = handPointer;
                //System.out.println("========= compare fail");
                //System.out.println("========= reset ref bit page number " + pageTable[previouspointer].getpageNum());
                pageTable[previouspointer].setreferenceBit(0);
                handPointer++;
                //System.out.println("========= victimPointer B = " + victimPointer);
            }
        }

         victimPointer = handPointer;
         //System.out.println("========= hand pointer to victim pointer #" + victimPointer);
         handPointer++;

         if (handPointer == frameCount) {
            handPointer = 0;
        }
    }

    // find the minimum 00 01 10 11 in which frame position
    public int anchor() {

        int index = -1;
        
        int min = 5; // high value to start with
        for (int i = 0; i < pageTable.length; i++) {
            //System.out.println(pageTable[i].getClassRM());
            if (pageTable[i].getClassRM() < min) {
                min =  pageTable[i].getClassRM();
                index = i;
            }
        }

        //System.out.println("========= peek " + index);
        //System.out.println("========= handPointer " + handPointer);
        //System.out.println("========= peek Class " + pageTable[index].getClassRM());
        //System.out.println("========= handPointer Class " + pageTable[handPointer].getClassRM());

        if (pageTable[index].getClassRM() == pageTable[handPointer].getClassRM()) {
            index = handPointer;
        }

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
        if (laststatusModified == 1){
            index = true;
        }

        return index;
    }

    /* custom-defined page
     */
    class pageEntry {

        private int pageNum;
        private int pagetoFrame;
        private int modifiedBit;
        private String processBit;
        private int ageBit;
        private int localptr;
        private int referenceBit;
        private int classRM;

        public pageEntry() {
            pageNum = -1;       //-1 is empty bit
            pagetoFrame = -1;   //-1 is empty bit
            modifiedBit = 0;    //0 false 1 true
            processBit = "";    
            ageBit = -1;        //-1 is initial age
            localptr = -1;
            referenceBit = 0;
            classRM = 0;             
        }
        
        public int getpageNum() {
            return pageNum;
        }
            
        public int getpagetoFrame() {
            return pagetoFrame;
        }             
        
        public int getmodifiedBit() {
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

        public int getreferenceBit() {
            return referenceBit;
        }        

        public void setpageNum(int pageNum) {
            this.pageNum = pageNum;
        }
            
        public void setpagetoFrame(int pagetoFrame) {
            this.pagetoFrame = pagetoFrame;
        }            
            
        public void setmodifiedBit(int modifiedBit) {
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

        public void setreferenceBit(int referenceBit) {
            this.referenceBit = referenceBit;
        }

        // 00 - 0 , 01 - 1, 10 - 2, 11 - 3
        public int getClassRM() {
            String s1 = Integer.toString(referenceBit);
            String s2 = Integer.toString(modifiedBit);
            String s3 =  s1 + s2; 
            int classRM = Integer.parseInt(s3, 2);
            return classRM;
        }
    }

}