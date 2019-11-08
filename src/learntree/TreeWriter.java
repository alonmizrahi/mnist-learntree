package learntree;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class TreeWriter {
	public static void write(Object obj, String filename) {
		if(obj == null || !(obj instanceof Serializable))
			throw new RuntimeException("attempted to write unserializable object " + obj.toString() + " to " + filename);
			
		//delete manually if already exists
		File file = new File(filename);
		if(file.exists())
			file.delete();
		
		//write to file
	    try {
	    	FileOutputStream fileOut = new FileOutputStream(filename);
	    	ObjectOutputStream out = new ObjectOutputStream(fileOut);
	    	out.writeObject(obj);
	    	out.close();
	    	fileOut.close();
	    }
	    catch (IOException e) {
	          throw new RuntimeException("IOException: " + e.getMessage());
	    }
	}
}
