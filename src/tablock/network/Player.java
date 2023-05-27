package tablock.network;

public class Player
{
    public double x;
    public double y;
    public double rotationAngle;
    public byte animationType;
    public byte animationDirection;
    public byte jumpProgress;

    byte[][] encode()
    {
        return new byte[][]{DataType.DOUBLE.encode(x), DataType.DOUBLE.encode(y), DataType.DOUBLE.encode(rotationAngle), DataType.BYTE.encode(animationType), DataType.BYTE.encode(animationDirection), DataType.BYTE.encode(jumpProgress)};
    }

    void decode(Object[] decodedData)
    {
        x = (double) decodedData[0];
        y = (double) decodedData[1];
        rotationAngle = (double) decodedData[2];
        animationType = (byte) decodedData[3];
        animationDirection = (byte) decodedData[4];
        jumpProgress = (byte) decodedData[5];
    }
}