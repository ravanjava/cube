package com.gap.pem.cds.stores;

/**
 */
public abstract class LookupStore extends GenericStore 
{
    private static final long serialVersionUID = 5424428924135958501L;

    protected IntStore keyStore;
    protected int currentID = 0;

    public LookupStore()
    {
		super();
    }

    public LookupStore(int initialChunkCount, int inputChunkSize)
    {
        super(initialChunkCount, inputChunkSize);
    }

	@Override
    protected void allocateStore (int chunkCount, int inputChunkSize)
	{
        validIndex = -1;
        keyStore = new IntStore(chunkCount, inputChunkSize);
	}

    @Override
    public String toString() {
        return "LookupStore{" +
                "keyStore=" + keyStore +
                ", currentID=" + currentID +
                '}';
    }
}

