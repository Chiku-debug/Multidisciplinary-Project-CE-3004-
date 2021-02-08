import java.io.File; // Import the File class
import java.io.FileNotFoundException; // Import this class to handle errors
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Scanner; // Import the Scanner class to read text files

public class ReadMap {
    private String pathName="";
    public ReadMap(){

    }
    public ReadMap(String pathName){
        this.pathName = pathName;
    }

    public ArrayList<String> fileToArray(){
        ArrayList<String> rows = new ArrayList<>();
        try {
            File myObj = new File(pathName);
          //File myObj = new File("testArena1.txt");
          Scanner myReader = new Scanner(myObj);
          while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            rows.add(data);
            //System.out.println(data);
          }
          //System.out.println(rows);
          myReader.close();
          
        } catch (FileNotFoundException e) {
          System.out.println("An error occurred.");
          e.printStackTrace();
        }
        return rows;
    }

    public String fileToString(){
      StringBuilder fileStr = new StringBuilder();
      String finalStr;
      try {
          File myObj = new File(pathName);
        Scanner myReader = new Scanner(myObj);
       
        while (myReader.hasNextLine()) {
          String data = myReader.nextLine();
          fileStr.append(data);
          //System.out.println(data);
        }
        System.out.println(myObj);
        myReader.close();
      } catch (FileNotFoundException e) {
        System.out.println("An error occurred.");
        e.printStackTrace();
      }
      finalStr =  fileStr.toString();
      return finalStr;
  }

  
  public static void main(String[] args) {
    //ArrayList<String> mapArr = new ArrayList<>();
    String mapStr;

    ReadMap map1 = new ReadMap();
    map1.pathName="C:\\Users\\whate\\Documents\\GitHub\\MDP\\MDP-Algorithm-Drafts\\MapFiles\\"+"testArena5.txt";
    String inputStr = map1.fileToString();
    String hexString = new BigInteger(inputStr, 2).toString(16);

    //mapStr = map1.fileToString();
    System.out.println(hexString);
    
    //mapArr = map1.fileToArray();
    
    /* int n=0;
    for (String row : mapArr){
        int j = 0;
        for (String pos:row.split("")){
            if(pos.equals("1")){
                System.out.println(j+","+n);
            }
            j++;
        }
        n++;
    }  */
  }
}
