package tablock.core;

import tablock.network.DataType;

public class Player
{
    public double x;
    public double y;
    public double rotationAngle;
    public byte animationType;
    public byte animationDirection;
    public byte jumpProgress;

    public byte[][] encode()
    {
        return new byte[][]{DataType.DOUBLE.encode(x), DataType.DOUBLE.encode(y), DataType.DOUBLE.encode(rotationAngle), DataType.BYTE.encode(animationType), DataType.BYTE.encode(animationDirection), DataType.BYTE.encode(jumpProgress)};
    }

    public void update(Object[] playerData)
    {
        x = (double) playerData[0];
        y = (double) playerData[1];
        rotationAngle = (double) playerData[2];
        animationType = (byte) playerData[3];
        animationDirection = (byte) playerData[4];
        jumpProgress = (byte) playerData[5];
    }
}