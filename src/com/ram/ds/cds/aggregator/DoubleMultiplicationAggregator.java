package com.ram.ds.cds.aggregator;

import com.ram.ds.cds.stores.IDoubleArrayStore;

public class DoubleMultiplicationAggregator implements Aggregator{
    double aggregationResult;
    int forecastHorizon;
    IDoubleArrayStore firstStore;
    IDoubleArrayStore secondStore;

    public DoubleMultiplicationAggregator(IDoubleArrayStore firstStore, IDoubleArrayStore secondStore, int forecastHorizon) {
        this.forecastHorizon = forecastHorizon;
        this.firstStore = firstStore;
        this.secondStore = secondStore;
    }

    @Override
    public void accumulate(int dfuIndex) {
        double[] firstValue = firstStore.getElement(dfuIndex);
        double[] secondValue = secondStore.getElement(dfuIndex);
        for (int j = 0; j < forecastHorizon; j++) {
            aggregationResult += firstValue[j] * secondValue[j];
        }
    }

    public double getResult()
    {
        return aggregationResult;
    }

    @Override
    public String toString() {
        return "DoubleMultiplicationAggregator{" +
                "aggregationResult=" + aggregationResult +
                ", forecastHorizon=" + forecastHorizon +
                ", firstStore=" + firstStore +
                ", secondStore=" + secondStore +
                '}';
    }
}

