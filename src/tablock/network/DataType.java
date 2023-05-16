package tablock.network;

public enum DataType
{
    INTEGER
    {
        @Override
        byte[] toByteArray(Object data)
        {
            int integer = (int) data;

            return new byte[]{(byte) (integer >> 24), (byte) (integer >> 16), (byte) (integer >> 8), (byte) integer};
        }

        @Override
        Object decode(byte[] data)
        {
            return (data[0] & 255) << 24 | (data[1] & 255) << 16 | (data[2] & 255) << 8 | (data[3] & 255);
        }
    },

    STRING
    {
        @Override
        byte[] toByteArray(Object data)
        {
            return ((String) data).getBytes();
        }

        @Override
        Object decode(byte[] data)
        {
            return new String(data);
        }
    },

    BYTE_ARRAY
    {
        @Override
        byte[] toByteArray(Object data)
        {
            return (byte[]) data;
        }

        @Override
        Object decode(byte[] data)
        {
            return data;
        }
    };

    abstract byte[] toByteArray(Object data);
    abstract Object decode(byte[] data);

    public byte[] encode(Object data)
    {
        byte[] bytes = toByteArray(data);
        byte[] encodedData = new byte[bytes.length + 3];

        encodedData[0] = (byte) (bytes.length >> 8);
        encodedData[1] = (byte) bytes.length;

        System.arraycopy(bytes, 0, encodedData, 2, bytes.length);

        encodedData[encodedData.length - 1] = (byte) ordinal();

        return encodedData;
    }
}