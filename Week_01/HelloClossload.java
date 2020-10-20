import io.jsonwebtoken.io.IOException;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * @author: wangxy
 */
public class HelloClossload extends ClassLoader {

    public static void main(String[] args) {
        try {
            Object hello = new HelloClossload().findClass("Hello").newInstance();
            hello.getClass().getDeclaredMethod("hello").invoke(hello);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }


    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        File file = new File("/Hello.xlass");
        byte[] fileByteArray = getFileByteArray(file);
        byte[] resByte = new byte[fileByteArray.length];
        for (int i = 0; i < fileByteArray.length; i++) {
            resByte[i] = (byte)(255-fileByteArray[i]);
        }
        return defineClass(name,resByte,0,resByte.length);
    }

    /**
     * 将文件转换成二进byte数组
     * @param file
     * @return
     */
    public static byte[] getFileByteArray(File file) {
        long fileSize = file.length();
        if (fileSize > Integer.MAX_VALUE) {
            System.out.println("file too big...");
            return null;
        }
        byte[] buffer = null;
        try (FileInputStream fi = new FileInputStream(file)) {
            buffer = new byte[(int) fileSize];
            int offset = 0;
            int numRead = 0;
            while (offset < buffer.length
                    && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
                offset += numRead;
            }
            // 确保所有数据均被读取
            if (offset != buffer.length) {
                throw new IOException("Could not completely read file "
                        + file.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer;
    }
}
