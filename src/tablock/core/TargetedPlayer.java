package tablock.core;

public class TargetedPlayer
{
    public final TargetedValue x = new TargetedValue();
    public final TargetedValue y = new TargetedValue();
    public final TargetedValue rotationAngle = new TargetedValue();
    private byte animationType;
    private byte animationDirection;
    private byte jumpProgress;

    public TargetedPlayer(Object[] playerData)
    {
        update(playerData);

        x.setCurrentValue((double) playerData[0]);
        y.setCurrentValue((double) playerData[1]);
        rotationAngle.setCurrentValue((double) playerData[2]);
    }

    public void update(Object[] playerData)
    {
        double x = (double) playerData[0];
        double y = (double) playerData[1];
        double rotationAngle = (double) playerData[2];
        byte animationType = (byte) playerData[3];
        byte animationDirection = (byte) playerData[4];
        byte jumpProgress = (byte) playerData[5];

        this.x.setTargetValue(x);
        this.y.setTargetValue(y);
        this.rotationAngle.setTargetValue(rotationAngle);

        if(x == 0 && y == 0 && rotationAngle == 0 && animationType == 0 && animationDirection == 0 && jumpProgress == 0)
        {
            this.x.setCurrentValue(0);
            this.y.setCurrentValue(0);
            this.rotationAngle.setCurrentValue(0);
        }

        this.animationType = animationType;
        this.animationDirection = animationDirection;
        this.jumpProgress = jumpProgress;
    }

    public void pursueTargetValues()
    {
        x.pursueTargetValue();
        y.pursueTargetValue();
        rotationAngle.pursueTargetValue();
    }

    public byte getAnimationType()
    {
        return animationType;
    }

    public byte getAnimationDirection()
    {
        return animationDirection;
    }

    public byte getJumpProgress()
    {
        return jumpProgress;
    }
}