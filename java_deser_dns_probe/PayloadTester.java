import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class PayloadTester
{
    public static void main(String[] args) throws Exception 
    {
        FileInputStream fis = new FileInputStream("payload.ser");
        ObjectInputStream ois = new ObjectInputStream(fis);
        Object o = ois.readObject();
        ois.close();
        fis.close();
    }
}
