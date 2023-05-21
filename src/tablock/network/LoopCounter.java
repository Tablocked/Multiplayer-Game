package tablock.network;

class LoopCounter
{
    private long timeDuringCountStart = System.nanoTime();
    private int loopCount;
    private int loopsPerSecond;

    void increment()
    {
        loopCount++;
    }

    int computeLoopsPerSecond()
    {
        if(System.nanoTime() - timeDuringCountStart > 1e9)
        {
            timeDuringCountStart = System.nanoTime();
            loopsPerSecond = loopCount;
            loopCount = 0;
        }

        return loopsPerSecond;
    }
}