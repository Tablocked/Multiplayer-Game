package tablock.core;

import tablock.network.DataType;

public class Player
{
    public double x;
    public double y;
    public double rotationAngle;
    public byte jumpProgress;
    public byte animationType;
    public byte animationDirection;
    public boolean reset;

    public byte[][] encode()
    {
        return new byte[][]{DataType.DOUBLE.encode(x), DataType.DOUBLE.encode(y), DataType.DOUBLE.encode(rotationAngle), DataType.BYTE.encode(jumpProgress), DataType.BYTE.encode(animationType), DataType.BYTE.encode(animationDirection), DataType.BOOLEAN.encode(reset)};
    }

    public void update(Object[] playerData)
    {
        x = (double) playerData[0];
        y = (double) playerData[1];
        rotationAngle = (double) playerData[2];
        jumpProgress = (byte) playerData[3];
        animationType = (byte) playerData[4];
        animationDirection = (byte) playerData[5];
        reset = (boolean) playerData[6];
    }
}