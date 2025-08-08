package net.idea.wrapper;

public class StreamingMeanLong {
    private long count = 0;
    private long mean = 0;

    // Add a new value to the stream
    public void add(long value) {
        count++;
        mean += (value - mean) / count;
    }

    // Get the current mean
    public long getMean() {
        return mean;
    }

    // Get how many values have been processed
    public long getCount() {
        return count;
    }
}

