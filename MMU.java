/*  Luqhasal
*   Operating Systems
*/

/**
* Interface for Memory Management Unit.
* The memory management unit implements the LA to PA translation
* with the checkInMemory. Page faults will cause pages to be loaded
* into free frames (when the number of page faults is not above the
* exisiting frame number). When the memory is full the replacement
* algorithm select the frame into which the page is loaded. o
*
*/

public interface MMU {
    public void readMemory(int page_number);
    public void writeMemory(int page_number);
    
    public int checkInMemory(int page_number);
    public void allocateFrame(int page_number);

    public int selectVictim(int page_number);
    public boolean lastVictimStatus();
    
}
