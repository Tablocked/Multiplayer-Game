package tablock.core;

import java.io.File;

public class FilePointer
{
    private File file;

    public FilePointer(File file)
    {
        this.file = file;
    }

    public boolean changeFilePath(String newPath)
    {
        File newFile = new File(newPath);

        if(file.renameTo(newFile))
        {
            file = newFile;

            return true;
        }

        return false;
    }

    public File getFile()
    {
        return file;
    }
}