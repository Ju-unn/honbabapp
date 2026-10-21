import java.util.*;
public class RowsCheck {
    static List<Integer> rowsFor(int n){
        List<Integer> r=new ArrayList<>();
        if(n<=0) return r;
        if(n<=3){ r.add(n); return r; }
        int k=(n+2)/3, base=n/k, rem=n%k;
        for(int i=0;i<k;i++) r.add(i<rem?base+1:base);
        return r;
    }
    public static void main(String[] a){
        int[][] exp={{1},{2},{3},{2,2},{3,2},{3,3},{3,2,2},{3,3,2},{3,3,3},{3,3,2,2}};
        for(int n=1;n<=10;n++){
            List<Integer> got=rowsFor(n);
            List<Integer> e=new ArrayList<>(); for(int x:exp[n-1]) e.add(x);
            int sum=got.stream().mapToInt(Integer::intValue).sum();
            if(!got.equals(e) || sum!=n) throw new AssertionError("n="+n+" got="+got+" exp="+e);
            System.out.println("n="+n+" -> "+got+" OK");
        }
        System.out.println("ALL PASS");
    }
}
