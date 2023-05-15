package tablock.network;

public enum DataType
{
    INTEGER
    {
        @Override
        byte[] encode(Object data)
        {
            int integer = (int) data;

            return encode(new byte[]
            {
                (byte) ((integer >> 24) & 0xff),
                (byte) ((integer >> 16) & 0xff),
                (byte) ((integer >> 8) & 0xff),
                (byte) ((integer) & 0xff),
            });
        }

        @Override
        Object decode(byte[] data)
        {
            return
            (
                (0xff & data[0]) << 24  |
                (0xff & data[1]) << 16  |
                (0xff & data[2]) << 8   |
                (0xff & data[3])
            );
        }
    },

    STRING
    {
        @Override
        byte[] encode(Object data)
        {
            return encode(((String) data).getBytes());
        }

        @Override
        Object decode(byte[] data)
        {
            return new String(data);
        }
    };

    abstract byte[] encode(Object data);
    abstract Object decode(byte[] data);

    byte[] encode(byte[] data)
    {
        byte[] encodedData = new byte[data.length + 2];

        encodedData[0] = (byte) data.length;

        System.arraycopy(data, 0, encodedData, 1, data.length);

        encodedData[encodedData.length - 1] = (byte) ordinal();

        return encodedData;
    }
}