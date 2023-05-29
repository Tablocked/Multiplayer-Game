package tablock.core;

public class TargetedValue
{
    private double targetValue;
    private double currentValue;
    private long timeDuringTargetValueSet;
    private double valueDuringTargetValueSet;

    void pursueTargetValue()
    {
        if(currentValue != targetValue)
            currentValue = VectorMath.computeLinearEquation(timeDuringTargetValueSet, valueDuringTargetValueSet, timeDuringTargetValueSet + 80, targetValue, System.currentTimeMillis());
    }

    void resetValues()
    {
        setTargetValue(0);
        setCurrentValue(0);

        valueDuringTargetValueSet = 0;
    }

    void setTargetValue(double targetValue)
    {
        this.targetValue = targetValue;

        timeDuringTargetValueSet = System.currentTimeMillis();
        valueDuringTargetValueSet = currentValue;
    }

    void setCurrentValue(double currentValue)
    {
        this.currentValue = currentValue;
    }

    public double get()
    {
        return currentValue;
    }
}