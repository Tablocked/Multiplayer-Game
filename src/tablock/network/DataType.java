package tablock.network;

import java.nio.ByteBuffer;

public enum DataType
{
    BYTE
    {
        @Override
        byte[] toByteArray(Object data)
        {
            return new byte[]{(byte) data};
        }

        @Override
        Object decode(byte[] data)
        {
            return data[0];
        }
    },

    BOOLEAN
    {
        @Override
        byte[] toByteArray(Object data)
        {
            return (boolean) data ? new byte[]{1} : new byte[]{0};
        }

        @Override
        Object decode(byte[] data)
        {
            return data[0] == 1;
        }
    },

    INTEGER
    {
        @Override
        byte[] toByteArray(Object data)
        {
            return ByteBuffer.allocate(4).putInt((int) data).array();
        }

        @Override
        Object decode(byte[] data)
        {
            return ByteBuffer.wrap(data).getInt();
        }
    },

    DOUBLE
    {
        @Override
        byte[] toByteArray(Object data)
        {
            return ByteBuffer.allocate(8).putDouble((double) data).array();
        }

        @Override
        Object decode(byte[] data)
        {
            return ByteBuffer.wrap(data).getDouble();
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