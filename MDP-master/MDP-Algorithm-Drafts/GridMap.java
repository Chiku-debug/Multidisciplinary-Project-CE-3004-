import java.awt.Color;
import java.math.BigInteger;
import java.util.ArrayList;
public class GridMap{  
    private ArrayList<GridCell> cellsArr = new ArrayList<>();
    private Integer rowNum = 20;
    private Integer colNum = 15;

    public GridMap(){
        //generates an unexplored gridmap (cellsArr)
        for (Integer r = rowNum; r>0 ; r--){
            for(Integer c = 1; c<colNum+1; c++){
                this.cellsArr.add(new GridCell(c, r));
                //System.out.println(c+","+r);
            }
        }    
    }

    public ArrayList<GridCell> getCellsArr(){
        return this.cellsArr;
    }

    public GridCell getCell(Integer position){
        return this.cellsArr.get(position);
    }

    public Integer coordToPos(Integer x, Integer y){
        //determines the position of the cell in cellsArr, based on given coordinate xy.
        Integer position = (this.rowNum - y )*colNum + x - 1;
        //Integer position = (this.rowNum-y)*colNum+x;

        return position;

    }
    public String part1String(){ //generates a padded binary string based on the GridMap - Explored/Unexplored
        String strPart1 = "11";
        for(GridCell cell: this.cellsArr){
            if(cell.isExplored()){
                strPart1 += "1";
            }
            else{
                strPart1 += "0";
            }
        }
        strPart1 += "11";
        return strPart1;
    }

    public String test2String(){
        String part2 = "";
        for(GridCell cell: this.cellsArr){
            if(cell.isExplored() && cell.isOccupied()){
                part2+="1";
            }
            else if(cell.isExplored() && !cell.isOccupied()){
                part2+="0";
            }
        }
        Integer length = part2.length();
        if(length%8!=0){
            for(int p = 0; p<8-length%8; p++){
                part2+="0";
            }
        }
        return binToHex(part2);
    }

    public String part2String(){//generates a padded binary string based on the GridMap - Occupied/Unoccupied
        String strPart2 = "";
        for(GridCell cell: this.cellsArr){
            if(cell.isExplored()){
                if(cell.isOccupied()){
                    strPart2+="1";
                }
                else{
                    strPart2+="0";
                }
            }   
        }
        //padding of string with 0s to fill up to bytes.
        Integer length = strPart2.length();
        if(length%8!=0){
            for(int p = 0; p<8-length%8; p++){
                strPart2+="0";
            }
        }


        return strPart2;
    }

    public ArrayList <String> splitBytes(String inputString){
        ArrayList <String> splitList = new ArrayList<>();
        Integer splitSize = 4;
        for(int i = 0; i<inputString.length();i+=splitSize){
            splitList.add(inputString.substring(i,Math.min(inputString.length(),i+splitSize)));
        }
        return splitList;
    }
    public String binToHex(String inputStr){ //converts given binary string to hex.
        ArrayList <String> splitList = splitBytes((inputStr));
        String finalStr = "";
        for (String str: splitList){
            finalStr+=new BigInteger(str, 2).toString(16);
        }
        //System.out.println("[binToHex]: "+finalStr);
        /* String hexString = new BigInteger(inputStr, 2).toString(16);
        System.out.println("[hexString]: "+hexString); 
        return hexString;*/
        return finalStr;
    }

    public String hexToBin(String hexStr){
        String binStr = new BigInteger(hexStr,16).toString(2);
        return binStr;
    }


    public void setMap(String strPart1, String strPart2){//Sets all cells in map with given part1 and part2 strings.
        //Will automatically remove strPart1 and strPart2 paddings! 

        if(strPart1.length()==304){//removes header and tail "11" padding from strPart1
            String sub1 = strPart1.substring(2,strPart1.length()-2);
            strPart1 = sub1;
        }
        Integer part2Count = 0;
        for (int i = 0; i<strPart1.length(); i++){//Set Explored/Unexplored
            GridCell currCell = this.cellsArr.get(i);
            //System.out.println("currCell: "+currCell.getX()+","+currCell.getY());
            char c = strPart1.charAt(i);
            if(c=='1'){ 
                currCell.setExplored();
                char c2 = strPart2.charAt(part2Count);
                if(c2=='1'){ //Set Occupied/Unoccupied
                    currCell.setOccupied();
                }
                else{
                    currCell.setUnoccupied();
                }
                part2Count ++;
            }
            else{
                currCell.setUnexplored();
            }
        }

    }

    public void setCell(Integer x, Integer y, boolean isExplored, boolean isOccupied){ //Sets individual cell in GridMap.
        GridCell selCell = this.cellsArr.get(this.coordToPos(x,y));
        if(isExplored){
            selCell.setExplored();
        }
        else{
            selCell.setUnexplored();
        }

        if(isOccupied){
            selCell.setOccupied();
        }
        else{
            selCell.setUnoccupied();
        }

    }
    public boolean isFinished(){
        boolean finished = true;
        for (GridCell cell : this.cellsArr){
            if(! cell.isExplored()){
                finished = false;
                break;
            }
        }
        return finished;
    }
    
    public void setMapFromHex(String hexStr){//takes in pre-formatted hex string (with padding)
        //sets this.cellsArr accordingly.
        String binStr = hexToBin(hexStr);
        String strPart1 = binStr.substring(0,304);
        String strPart2 = binStr.substring(304);
        this.setMap(strPart1, strPart2);
    }

    public void setMapFromBin(String binStr){ //takes in pre-formatted bin string (with padding)
        String strPart1 = binStr.substring(0,304);
        String strPart2 = binStr.substring(304);
        this.setMap(strPart1, strPart2);
    }

    public void resetMap(){
        for (GridCell c: this.cellsArr){
            c.setUnexplored();
            c.setUnoccupied();
        }
    }
    public static void main(String[] args) {
        //use this to test GridMap functions.

        GridMap map1 = new GridMap();
        System.out.println(map1.coordToPos(15, 20));

        
       /*  
       String str1 = "1111111111000000011111111000000011111111000000011111111000000011111111111111111111111111111100111111111111100111111111111100111111111111100111000111111100111000111111100111000111111100111000111111100001111111111100001111111111100001111111111100001111111111100001111111111100001110000011100001110000011111";
        String str2 = "0000000000000000000000010000000000000000000111001000000000000000000000000000000000000000000111000000000000000000000010000000000000000000000001100000000000000001110000000000000000000000000010000000000000000000";
        map1.setMap(str1,str2);
        System.out.println(map1.binToHex(map1.part1String()));
        map1.setCell(12, 20, true, false);
        System.out.println(map1.binToHex(map1.part1String()));
        System.out.println(map1.coordToPos(1, 19));
        String str1 = "1111111111000000011111111000000011111111000000011111111000000011111111111111111111111111111100111111111111100111111111111100111111111111100111000111111100111000111111100111000111111100111000111111100001111111111100001111111111100001111111111100001111111111100001111111111100001110000011100001110000011111";
        String str2 = "0000000000000000000000010000000000000000000111001000000000000000000000000000000000000000000111000000000000000000000010000000000000000000000001100000000000000001110000000000000000000000000010000000000000000000";
        map1.setMap(str1,str2);
        System.out.println(map1.part1String());
        System.out.println(map1.part2String());
        String str = map1.part1String();
        System.out.println(str);
        System.out.println("-------------------");
        System.out.println(map1.binToHex(str));
        map1.setMapPart1("1111111111000000011111111000000011111111000000011111111000000011111111111111111111111111111100111111111111100111111111111100111111111111100111000111111100111000111111100111000111111100111000111111100001111111111100001111111111100001111111111100001111111111100001111111111100001110000011100001110000011111");
        String str2 = map1.part1String();
        System.out.println(map1.part1String());
        map1.resetMap();
        System.out.println(map1.part1String()); 
         int position = map1.coordToPos(7, 5);
        GridCell cell = map1.cellsArr.get(position+2);
        System.out.println(cell.getX()+","+cell.getY());
        cell.setExplored();
        cell.setOccupied();
        str = map1.part1String();
        System.out.println(map1.binToHex(str));
        System.out.println(map1.part2String());
        System.out.println(map1.binToHex("1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111"));
        for(GridCell someCell: map1.cellsArr){
            someCell.setExplored();
        }
        System.out.println(map1.isFinished());
        map1.resetMap();
        System.out.println(map1.isFinished());
        System.out.println(map1.coordToPos(str,7,5)); */
        

        

    }
}