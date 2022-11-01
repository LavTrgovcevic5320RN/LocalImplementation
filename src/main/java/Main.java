import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {



    public static void main(String[] args) throws IOException {
        LocalImplementation l = LocalImplementation.getInstance();
        l.initialiseDirectory("SK", "C:\\Users\\Lav\\Desktop\\Adasdasd", 2048, 5,  "exe");
        l.create("A", "C:\\Users\\Lav\\Desktop\\Adasdasd\\SK\\", 5);
        l.create("B", "C:\\Users\\Lav\\Desktop\\Adasdasd\\SK\\", 5);
        File file1 = new File("C:\\Users\\Lav\\Desktop\\Adasdasd\\SK\\A\\1.txt");
        File file2 = new File("C:\\Users\\Lav\\Desktop\\Adasdasd\\SK\\A\\2.xlsx");
        file1.createNewFile();
        file2.createNewFile();

        l.rename("Marko", "C:\\Users\\Lav\\Desktop\\Adasdasd\\SK\\A");


        //l.moveFiles("#\\B", "#\\A\\1.txt", "#\\A\\2.xlsx");
//        System.out.println(BraceExpansion.expand("C:/{a,b,c}/{1..15}adc"));
    }


}
