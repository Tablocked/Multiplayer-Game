package tablock.core;

public class TargetedPlayer
{
    public final TargetedValue x = new TargetedValue();
    public final TargetedValue y = new TargetedValue();
    public final TargetedValue rotationAngle = new TargetedValue();
    public final TargetedValue jumpProgress = new TargetedValue();
    private byte animationType;
    private byte animationDirection;
    public String name = "UNKNOWN";

    public TargetedPlayer(Object[] playerData)
    {
        update(playerData);

        x.setCurrentValue((double) playerData[0]);
        y.setCurrentValue((double) playerData[1]);
        rotationAngle.setCurrentValue((double) playerData[2]);
        jumpProgress.setCurrentValue((byte) playerData[3]);
    }

    public void update(Object[] playerData)
    {
        if((boolean) playerData[6])
        {
            x.resetValues();
            y.resetValues();
            rotationAngle.resetValues();
            jumpProgress.resetValues();

            animationType = 0;
            animationDirection = 0;
        }
        else
        {
            x.setTargetValue((double) playerData[0]);
            y.setTargetValue((double) playerData[1]);
            rotationAngle.setTargetValue((double) playerData[2]);
            jumpProgress.setTargetValue((byte) playerData[3]);

            animationType = (byte) playerData[4];
            animationDirection = (byte) playerData[5];
        }
    }

    public void pursueTargetValues()
    {
        x.pursueTargetValue();
        y.pursueTargetValue();
        rotationAngle.pursueTargetValue();
        jumpProgress.pursueTargetValue();
    }

    public byte getAnimationType()
    {
        return animationType;
    }

    public byte getAnimationDirection()
    {
        return animationDirection;
    }

    public String getName()
    {
        return name;
    }
}