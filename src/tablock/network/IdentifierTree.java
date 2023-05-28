package tablock.network;

import org.dyn4j.BinarySearchTree;

class IdentifierTree extends BinarySearchTree<Byte>
{
    private byte nextIdentifier;

    byte allocateNextIdentifier()
    {
        if(size() + 1 > 256)
            throw new RuntimeException("The maximum identifier limit of 256 was exceeded.");

        if(nextIdentifier == 0)
            nextIdentifier = 1;

        byte allocatedIdentifier = nextIdentifier;

        insert(nextIdentifier);

        do
            nextIdentifier++;
        while(contains(nextIdentifier));

        return allocatedIdentifier;
    }
}