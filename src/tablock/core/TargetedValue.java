package tablock.core;

public class TargetedValue
{
    private double targetValue;
    private double currentValue;
    private long timeDuringTargetValueSet;

    void pursueTargetValue()
    {
        if(currentValue != targetValue)
        {
            double value = VectorMath.computeLinearEquation(timeDuringTargetValueSet, currentValue, timeDuringTargetValueSet + 100, targetValue, System.currentTimeMillis());

            currentValue = currentValue < targetValue ? Math.min(value, targetValue) : Math.max(value, targetValue);
        }
    }

    void setTargetValue(double targetValue)
    {
        this.targetValue = targetValue;

        timeDuringTargetValueSet = System.currentTimeMillis();
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