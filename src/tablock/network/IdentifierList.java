package tablock.network;

import java.util.ArrayList;

class IdentifierList<T extends Identifier>
{
    final ArrayList<T> list = new ArrayList<>();
    private final IdentifierTree identifierTree = new IdentifierTree();

    void add(T identifier)
    {
        list.add(identifier);

        identifier.identifier = identifierTree.allocateNextIdentifier();
    }

    void remove(T identifier)
    {
        list.remove(identifier);

        identifierTree.remove(identifier.identifier);
    }
}