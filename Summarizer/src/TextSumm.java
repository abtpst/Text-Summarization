import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static gate.Utils.inDocumentOrder;
import gate.*;
import static gate.util.persistence.PersistenceManager.loadObjectFromFile;

public class TextSumm {

    public static String HOME_PATH = "";
	
    public TextSumm() throws Exception{
			Gate.init();
			File gateHome = Gate.getGateHome();
			HOME_PATH = gateHome.getCanonicalPath() + "/";
			
			//System.out.println("Home path: "+ HOME_PATH);
			if (Gate.getGateHome() == null)
				Gate.setGateHome(gateHome);
			
			
			File pluginsHome = new File(gateHome, "plugins");
			//Register all the plugins that your program will need
			Gate.getCreoleRegister().registerDirectories(
					new File(pluginsHome, "ANNIE")
							.toURI().toURL());
	 }
	 
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
				
		new TextSumm();
		//Point it to where your gapp file resides on your hard drive
		 gate.CorpusController c = ((gate.CorpusController)loadObjectFromFile(new java.io.File("C:/Program Files (x86)/GATE_Developer_7.1/A/2/tsum.gapp")));
			Corpus corpus = (Corpus) Factory.createResource("gate.corpora.CorpusImpl");
			//Point it to whichever folder contains your documents
			URL dir = new File("C:/Program Files (x86)/GATE_Developer_7.1/A/2/Text").toURI().toURL();
			corpus.populate(dir, null, "UTF-8", false); //set the encoding to whatever is the encoding of your files
			
			c.setCorpus(corpus);
			c.execute();
			
			gate.Document tempDoc = null;
			
			int paracount=0;		// We shall keep a count of how many NVN annotations we receive. The we shall use this value to extract the top nouns
			
			HashMap<String, Integer> nouncounter = new HashMap<String, Integer>();
			
			HashMap<String, Integer> firstnouns = new HashMap<String, Integer>();
			
			HashMap<String, Integer> noundf = new HashMap<String, Integer>();
			
			String fv = null;
			
			//This is how you can access the annotations created by your gate application
			for (int i = 0; i < corpus.size(); i++)
			{
				tempDoc = (gate.Document)corpus.get(i);
				
				for(gate.Annotation a: tempDoc.getAnnotations().get("Sent")) // Get all the email ids of the thread, annotated as EmailID
				{
					paracount++;
					
					HashMap<String, Integer> nounkey = new HashMap<String, Integer>();
					
					try
					{
						
					nounkey=(HashMap<String, Integer>) a.getFeatures().get("Nouns");// Get all the nouns associated with this NVN annotation
					
					for (Map.Entry <String, Integer> entry : nounkey.entrySet())// Iterate over all the nouns in this NVN annotation
					{
						String nk = entry.getKey();
						int tf = entry.getValue();
						
						if(!nk.isEmpty())// If the noun obtained is not a blank string and it is not an email ID then proceed
						{
							
						if(nouncounter.containsKey(nk))// If this noun already appears in our nouncounter HashMap, then increase its count by 1
							nouncounter.put(nk, nouncounter.get(nk)+tf);
						else// If this noun does not appear in our nouncounter HashMap, meaning this is the first time we see it, set its count to 1
							nouncounter.put(nk, tf);
						
						if(noundf.containsKey(nk))// If this noun already appears in our nouncounter HashMap, then increase its count by 1
							noundf.put(nk, noundf.get(nk)+1);
						else// If this noun does not appear in our nouncounter HashMap, meaning this is the first time we see it, set its count to 1
							noundf.put(nk, 1);
						
						}
					
					}
					
					}
					catch(NullPointerException e)
					{}
				}
				
				for(gate.Annotation a: tempDoc.getAnnotations().get("First")) // Get all the email ids of the thread, annotated as EmailID
				{
					HashMap<String, Integer> nounkey = new HashMap<String, Integer>();
					
					try
					{
						fv = a.getFeatures().get("Value").toString();	
						
						nounkey=(HashMap<String, Integer>) a.getFeatures().get("Nouns");// Get all the nouns associated with this NVN annotation
						
						for ( Map.Entry<String, Integer> entry : nounkey.entrySet()) 
						{
							String nk = entry.getKey();
							
							if(noundf.containsKey(nk))// If this noun already appears in our nouncounter HashMap, then increase its count by 1
								noundf.put(nk, noundf.get(nk)+1);
							else// If this noun does not appear in our nouncounter HashMap, meaning this is the first time we see it, set its count to 1
								noundf.put(nk, 1);
						}
					}
					catch(NullPointerException e)
					{}
				}
				
			}
			
			 String nou;
			 
			 for ( Map.Entry<String, Integer> entry : firstnouns.entrySet()) 
			 {
	        	nou = entry.getKey();
	        	
	        	if(nouncounter.containsKey(nou))
	        	{
	        		nouncounter.put(nou, nouncounter.get(nou)+firstnouns.get(nou));
	        	}
			 }
			 
			ArrayList<String> nn = new ArrayList<String>();// This list represents the high scoring nouns that we will select
			
			nn = TextSumm.Scores(nouncounter, firstnouns, noundf, paracount+1);	
			
			ArrayList<String> nvn = new ArrayList<String>();
			
			List<Annotation> lis = null; // This list would be used to extract all the NVN annotations in the sequence that they appear in the original email

			lis = inDocumentOrder(tempDoc.getAnnotations().get("First"));	
			
			for(gate.Annotation a: lis) 	//Now iterate over the NVN annotations again, this time collecting the string values of the top nouns that we got above
			{
				HashMap<String, Integer> finalnounkey = (HashMap<String, Integer>) a.getFeatures().get("Nouns");	
				
				for(Map.Entry<String, Integer> entry : finalnounkey.entrySet())
				{
					String n = entry.getKey();
					
					if(nn.contains(n))
						if(!nvn.contains(a.getFeatures().get("Value").toString()))	// Add this string to the final summary only if it has not been seen before
							
							nvn.add(a.getFeatures().get("Value").toString());
							
				}
			}
			
			lis = inDocumentOrder(tempDoc.getAnnotations().get("Sent"));	
			
			for(gate.Annotation a: lis) 	//Now iterate over the NVN annotations again, this time collecting the string values of the top nouns that we got above
			{
				HashMap<String, Integer> finalnounkey = (HashMap<String, Integer>) a.getFeatures().get("Nouns");	
				
				try
				{
				for(Map.Entry<String, Integer> entry : finalnounkey.entrySet())
				{
					String n = entry.getKey();
					
					if(nn.contains(n))
						if(!nvn.contains(a.getFeatures().get("Value").toString()))	// Add this string to the final summary only if it has not been seen before
								nvn.add(a.getFeatures().get("Value").toString());
				}
				
				}
				catch (Exception e)
				{}
			}
			
			for(String jb : nvn)
			{			
				System.out.println(jb);	// Print out the summary
			}	
	}

	private static ArrayList<String> Scores(HashMap<String, Integer> nouncounter, HashMap<String, Integer> firstnouns, HashMap<String, Integer> noundf, int paracount) {
		 
		double globalcount = 0;
        
        globalcount = ((double)paracount/10.0);	// This count represents the number of the nouns which will be our candidates from the sorted HashMap
        	 
         HashMap<String, Double> nounscore = new HashMap<String, Double>();// HashMap for storing the tfidf scores of the nouns in the nouncounter
        
         for(Map.Entry <String, Integer> entry : nouncounter.entrySet())//Iterate over all the nouns in the nouncounter
         try
         {
            
        	 String term = entry.getKey();//Get the noun
        	 if(term.matches("[\\p{Alnum}]+"))//Eliminate non alphanumeric nouns
 	 		{ 
        	 int df=1;//df is set to one by default 
        			 
        	 int tf=entry.getValue();// Get the count associated with this noun. This will be the tf
	         
        	 double tfidf;
	         
        	 if(noundf.containsKey(term))
        		 df=noundf.get(term);
        	 
        	 tfidf=tf*Math.log10(paracount/df);//Calculate the tfidf score for this noun
        	
        	 if(firstnouns.containsKey(term))// If this noun also appears in firstnouns then assign it a higher score. 
        		 								//Store the final tfidf score in nounscores
        		 nounscore.put(term, tfidf+1);

        	 else//If not then, store the tfidf score in nounscores
        		 nounscore.put(term, tfidf);
         } 
        	
            
        }
        
       
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        ArrayList<String> its = new ArrayList<String>();// This is the list which is returned to the main function. It will hold all the high scoring nouns
 
        ValueComparator bvc =  new ValueComparator(nounscore);
        
        TreeMap<String,Double> sorted_map = new TreeMap<String,Double>(bvc);
        
        sorted_map.putAll(nounscore);// sorted_map now has the nounscore HashMap sorted by value in descending order
        
        int count = 0;// count represents what portion of the sorted_map entries we wish to take
        System.out.println(globalcount);
       for ( Map.Entry<String, Double> entry : sorted_map.entrySet()) 
		 {
    	   if(entry.getValue()>1)
        	System.out.println(entry.getKey()+" "+entry.getValue());
		 }
		for ( Map.Entry<String, Double> entry : sorted_map.entrySet()) 
		 {
			
			if(entry.getValue()>globalcount)//Here we will extract the top values and store them in the ArrayList which would be returned 
			{
				its.add(entry.getKey());// Add the entry to the list its
				
				count++;
				}
		 }
		return its;
	}

}

//Sort in descending order
class ValueComparator implements Comparator<String> {

	   Map<String, Double> base;
	   public ValueComparator(Map<String, Double> base) {
	       this.base = base;
	   }

	   // Note: this comparator imposes orderings that are inconsistent with equals.    
	   public int compare(String a, String b) {
	       if (base.get(a) >= base.get(b)) {
	           return -1;
	       } else {
	           return 1;
	       } // returning 0 would merge keys
	   }
	}

//Sort in ascending order
class ValueComparatorAsc implements Comparator<String> {

	   Map<String, Integer> base;
	   public ValueComparatorAsc(Map<String, Integer> base) {
	       this.base = base;
	   }

	   // Note: this comparator imposes orderings that are inconsistent with equals.    
	   public int compare(String a, String b) {
	       if (base.get(a) <= base.get(b)) {
	           return -1;
	       } else {
	           return 1;
	       } // returning 0 would merge keys
	   }
}
