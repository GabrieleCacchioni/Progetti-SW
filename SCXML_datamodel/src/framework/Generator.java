package framework;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;


public class Generator {
	public static List<String> stati = new ArrayList<String>();
	public static List<String> targets = new ArrayList<String>();
	public static List<String> data_ids = new ArrayList<String>();
	public static List<String> data_assign = new ArrayList<String>();
	// To pass arguments in Eclipse: "Run Configurations...", "Arguments", "Program Arguments"
	public static void main(String[] args) throws Exception {
		// checking input file name(s)
		if (args.length <= 0) {
			System.err.println("No args! Give the NAME(s) of the file(s) NAME.scxml containing the StateChart specification(s)");
			return;
		}
		for (String SCXMLModel : args) {
			File inputFile = new File(Conf.data_dir + Conf.filesep + SCXMLModel + Conf.scxml_extension);
//			System.out.println("inputFile absolutePath: " + inputFile.getAbsolutePath());
			if (!inputFile.exists()) {
				System.err.println("ERROR: the file " + inputFile.getAbsolutePath() + " for the model " + SCXMLModel + " does not exist.");
				System.err.println("The Generator is halted.");
				return;
			} else { // working on file
				System.out.println("Start generation of StateChart for model '" + SCXMLModel + "' specified in the file " + inputFile.getAbsolutePath());
				if (new File(Conf.source_dir + Conf.filesep + SCXMLModel).mkdir())
					System.out.println("First time for this StateChart, the package is created");
				else
					System.out.println("Modifying the existing package of the StateChart for model '" + SCXMLModel + "'");
//				Element documentRoot = getDocumentRoot(inputFile.getAbsolutePath());
				Element documentRoot = Common.getDocumentRoot(inputFile);
				if (!(SCXMLDocumentSyntaxOK(documentRoot))){
					System.err.println("Syntax error(s) in file " + inputFile.getAbsolutePath());
					System.err.println("The creation of this StateChart is interrupted.\n");
				}
				else
					stateChartGeneration(SCXMLModel, documentRoot, inputFile);
			Thread.sleep(100); // to avoid mixing printouts
			}
		}
	}

	private static boolean SCXMLDocumentSyntaxOK(Element pDocumentRoot) {
		boolean checkOK = true;
		// checking "data" node in the document
		Iterator<Element> anElement = pDocumentRoot.getDescendants(new ElementFilter("data"));
		while (anElement.hasNext()) {
			Element currentElement = anElement.next();
			if (currentElement.getAttribute("expr") == null) {
				System.err.println("ERROR: 'data' element '" + currentElement.getAttribute("id").getValue() + "' has no 'expr' attribute");
				checkOK = false;
			} else {
				if (currentElement.getAttribute("expr").getValue() == "") {
					System.err.println("ERROR: 'data' element '" + currentElement.getAttribute("id").getValue()
							+ "' has no value for the 'expr' attribute");
					checkOK = false;
				}
			}
		}
		checkOK= scxml_control_scxml(pDocumentRoot);
		return checkOK;
	}

	private static boolean check_initial(Element pDocumentRoot) {
		boolean check=true;
		if (pDocumentRoot.getAttribute("initial")==null){
			System.err.println("ERROR: initial not found");
			check = false;
		}
		else
			if (!(stati.contains(pDocumentRoot.getAttribute("initial").getValue()))){
				System.err.println("ERROR lo stato identificato come iniziale non esiste");
				check=false;
			}
		return check;
	}
	
	private static boolean scxml_control_scxml(Element pDocumentRoot) {

		boolean check=true;
		// controllo indirizzo e versione e deve avere almeno uno state oppure un parallel o un final
		
		if (pDocumentRoot.getAttribute("version")!=null){
			if (!pDocumentRoot.getAttribute("version").getValue().equals("1.0")) {
				System.err.println("ERROR version");
				check=false;
			}
		}
		else{
			System.err.println("ERROR: version not found");
			check=false;
		}
		if (!pDocumentRoot.getNamespace().getURI().equals("http://www.w3.org/2005/07/scxml")){
				System.err.println("ERROR errato valore per xmlns");
				check=false;
		}
		if (pDocumentRoot.getContent(new ElementFilter("state")).isEmpty() & 
				pDocumentRoot.getContent(new ElementFilter("parallel")).isEmpty() & 
				pDocumentRoot.getContent(new ElementFilter("final")).isEmpty()){
					System.err.println("ERROR non � presente n� uno state ne un parallel ne un final");
					check=false;
		}
		//Ora controlleremo i figli chiamando funzioni ricorsive
		Iterator<Element> datamodel =pDocumentRoot.getContent(new ElementFilter("datamodel")).iterator();
		if (datamodel.hasNext()) {
			check=check & check_datamodel(datamodel.next());
		}
		if (datamodel.hasNext()){
			System.err.println("ERROR pi� di un datamodel");
			check=false;
		}
		Iterator<Element> states =pDocumentRoot.getContent(new ElementFilter("state")).iterator();
		while (states.hasNext()) {
			check=check & check_state(states.next());
		}
		Iterator<Element> parallels =pDocumentRoot.getContent(new ElementFilter("parallel")).iterator();
		while (parallels.hasNext()) {
			check=check & check_parallel(parallels.next());
		}
		Iterator<Element> finals =pDocumentRoot.getContent(new ElementFilter("final")).iterator();
		while (finals.hasNext()) {
			check=check & check_final(finals.next());
		}
		check =check & check_targets();
		check =check & check_assign_in_data();
		check = check & check_initial(pDocumentRoot);
		return check;
	}
	
	private static boolean check_state(Element state) {
		List<String> figli = new ArrayList<String>();
		boolean flag=true;
		flag=check_state_id(state);
		Iterator<Element> onentry =state.getContent(new ElementFilter("onentry")).iterator();
		while (onentry.hasNext()) 
			flag=flag & check_onentry(onentry.next());		
		Iterator<Element> onexit =state.getContent(new ElementFilter("onexit")).iterator();
		while (onexit.hasNext()) 
			flag=flag & check_onexit(onexit.next());
		Iterator<Element> children =state.getContent(new ElementFilter("state")).iterator();
		figli.addAll(state.getContent(new ElementFilter("state")));
		while (children.hasNext())
			flag=flag & check_state(children.next());
		Iterator<Element> childrenp =state.getContent(new ElementFilter("parallel")).iterator();
		figli.addAll(state.getContent(new ElementFilter("parallel")));
		while (childrenp.hasNext())
			flag=flag & check_parallel(childrenp.next());
		Iterator<Element> childrenf =state.getContent(new ElementFilter("final")).iterator();
		figli.addAll(state.getContent(new ElementFilter("final")));
		while (childrenf.hasNext())
			flag=flag & check_final(childrenf.next());
		Iterator<Element> childrent =state.getContent(new ElementFilter("transition")).iterator();
		while (childrent.hasNext())
			flag=flag & check_trans(childrent.next());
		if ((state.getAttribute("id")!= null & state.getAttribute("initial")!=null) && !(figli.contains(state.getAttribute("initial").getValue()))){
			flag=false;
			System.err.println("ERROR lo stato "+ state.getAttributeValue("id") + "ha un initial="+state.getAttributeValue("initial")+" che non � un figlio");
		}
		return flag;
	}	
	private static boolean check_state_id(Element state){
		boolean flag=true;
		Element padre;
		if (state.getAttribute("id")== null){
			padre=state.getParentElement();
			if (padre.getAttribute("id")==null)
				System.err.println("ERROR esiste uno stato senza attributo id, con padre senza attributo id");		
			else
				System.err.println("ERROR esiste uno stato figlio di " + padre.getAttribute("id").getValue() + "senza attributo id");	
			flag=false;
		}
		else{
			if (stati.contains(state.getAttribute("id").getValue())){
				flag=false;
				System.err.println("ERROR esistono pi� stati chiamati "+state.getAttribute("id").getValue());
			}
			stati.add(state.getAttribute("id").getValue());
		}	
		return flag;
	}
	private static boolean check_assign_in_data(){
		boolean flag=true;
		data_assign.removeAll(data_ids);	
		if (!(data_assign.isEmpty())){
			Iterator<String> elenco=data_assign.iterator();
			while (elenco.hasNext()){
			System.err.println("ERROR c'� un assegnazione al data " + elenco.next() + " che per� non esiste");
			flag=false;
			}
		}
		data_assign.addAll(data_ids);
		return flag;
	}
	private static boolean check_targets(){
		boolean flag=true;
		targets.removeAll(stati);
		if (!(targets.isEmpty())){
			Iterator<String> elenco=targets.iterator();
			while (elenco.hasNext()){
			System.err.println("ERROR il target" + elenco.next() + " non � nessuno degli stati");
			flag=false;
			}
		}
		targets.addAll(stati);
		return flag;
	}
	
	private static boolean check_parallel(Element parallel) {
		boolean flag=true;
		flag=check_parallel_id(parallel);
		Iterator<Element> onentry =parallel.getContent(new ElementFilter("onentry")).iterator();
		while (onentry.hasNext())
			flag=flag & check_onentry(onentry.next());
		Iterator<Element> onexit =parallel.getContent(new ElementFilter("onexit")).iterator();
		while (onexit.hasNext()) 
			flag=flag & check_onexit(onexit.next());		
		Iterator<Element> children =parallel.getContent(new ElementFilter("state")).iterator();
		while (children.hasNext())
			flag=flag & check_state(children.next());
		Iterator<Element> childrenp =parallel.getContent(new ElementFilter("parallel")).iterator();
		while (childrenp.hasNext())
			flag=flag & check_parallel(childrenp.next());
		Iterator<Element> childrent =parallel.getContent(new ElementFilter("transition")).iterator();
		while (childrent.hasNext())
			flag=flag & check_trans(childrent.next());
		return flag;
	}	
	private static boolean check_parallel_id(Element parallel){
		boolean flag=true;
		Element padre;
		if (parallel.getAttribute("id")== null){
			padre=parallel.getParentElement();
			if (padre.getAttribute("id")==null)
				System.err.println("ERROR esiste uno stato (parallel) senza attributo id, con padre senza attributo id");		
			else
				System.err.println("ERROR esiste uno stato (parallel) figlio di " + padre.getAttribute("id").getValue() + "senza attributo id");	
			flag=false;
		}
		else{
			if (stati.contains(parallel.getAttribute("id").getValue())){
				flag=false;
				System.err.println("ERROR esistono pi� stati chiamati "+parallel.getAttribute("id").getValue());
			}
			stati.add(parallel.getAttribute("id").getValue());
		}	
		return flag;
	}
	
	private static boolean check_datamodel(Element datamodel) {
		boolean flag=true;
		Iterator<Element> datas =datamodel.getContent(new ElementFilter("data")).iterator();
		while (datas.hasNext())
			flag=flag & check_data(datas.next());
		return flag;
	}	
	private static boolean check_data(Element dato){
		boolean check_data_flag=true;
		
		check_data_flag = check_data_id_single(dato) && check_data_id(dato) ;
		
		check_data_flag = check_data_flag & check_data_src_expr(dato);
		
		data_ids.add(dato.getAttribute("id").getValue());
		
		return check_data_flag;
	}
	private static boolean check_data_src_expr(Element dato) {
		if (dato.getAttribute("src")!=null & dato.getAttribute("expr")!=null){
			System.err.println("ERROR un data non puo avere src E expr");
			return false;}
		else
			return true;
	}
	private static boolean check_data_id(Element dato) {
	if (dato.getAttribute("id")==null){
		System.err.println("ERROR un data non ha id");
		return false;}
	else
		if (dato.getAttribute("id").getValue() == "") {
			System.err.println("ERROR: 'data' element with id = '" + dato.getAttribute("id").getValue()
					+ "' has no value for the attribute");
			return false;
		}
			else
				return true;
	} 
	private static boolean check_data_id_single(Element dato) {
		if (data_ids.contains(dato.getAttributeValue("id"))){
			System.err.println("ERROR piu data hanno nome"+dato.getAttributeValue("id"));
			return false;
		}
			else
				return true;
	}
	
	private static boolean check_trans(Element trans) {
		boolean flag=true;
		Element padre;
		if (trans.getAttribute("event")==null & trans.getAttribute("cond")==null & trans.getAttribute("target")==null){
			padre=trans.getParentElement();
			if (padre.getAttribute("id")==null)
				System.err.println("ERROR una transizione di uno stato senza nome non ha n� target n� event n� cond");
			else
				System.err.println("ERROR una transizione dello stato " + padre.getAttributeValue("id")+ " non ha n� target n� event n� cond");	
			flag=false;
		}
		if (trans.getAttribute("target")!=null){
			targets.add(trans.getAttribute("target").getValue());
		}
		
		Iterator<Element> assigns =trans.getContent(new ElementFilter("assign")).iterator();
		while (assigns.hasNext()) {
			flag=flag & check_assign(assigns.next());
		}
		Iterator<Element> logs =trans.getContent(new ElementFilter("log")).iterator();
		while (logs.hasNext()) {
			flag=flag & check_log(logs.next());
		}
		return flag;
	}	
	
	private static boolean check_final(Element finale) {
		boolean flag=true;
		if (finale.getAttribute("id")!=null)
			stati.add(finale.getAttribute("id").getValue());
		else
			stati.add("final");
		Iterator<Element> onentry =finale.getContent(new ElementFilter("onentry")).iterator();
		while (onentry.hasNext()) {
			flag=flag & check_onentry(onentry.next());
		}		
		Iterator<Element> onexit =finale.getContent(new ElementFilter("onexit")).iterator();
		while (onexit.hasNext()) {
			flag=flag & check_onexit(onexit.next());
		}
		return flag;
	}	
	private static boolean check_onentry(Element onentry) {
		boolean flag=true;
		Iterator<Element> assigns =onentry.getContent(new ElementFilter("assign")).iterator();
		while (assigns.hasNext()) {
			flag=flag & check_assign(assigns.next());
		}
		Iterator<Element> logs =onentry.getContent(new ElementFilter("log")).iterator();
		while (logs.hasNext()) {
			flag=flag & check_log(logs.next());
		}
		return flag;
	}	
	private static boolean check_onexit(Element onexit) {
		boolean flag=true;
		Iterator<Element> assigns =onexit.getContent(new ElementFilter("assign")).iterator();
		while (assigns.hasNext()) {
			flag=flag & check_assign(assigns.next());
		}
		Iterator<Element> logs =onexit.getContent(new ElementFilter("log")).iterator();
		while (logs.hasNext()) {
			flag=flag & check_log(logs.next());
		}
		return flag;
	}	
	
	private static boolean check_assign(Element assegna) {
		boolean flag=true;
		Element padre;
		if (assegna.getAttribute("name")==null){
			// usare il metodo org.jdom.output.XMLOutputter.outputString ??
			padre=assegna.getParentElement().getParentElement();
			if (padre.getAttribute("id")==null)
				System.err.println("ERROR assegnazione relativa a uno stato senza id non ha l'attributo name");	
			else
				System.err.println("ERROR Assegnazione relativa allo stato " + padre.getAttribute("id").getValue() + "non ha l'attributo name");	
			flag=false;
		}else{
			data_assign.add(assegna.getAttribute("name").getValue());
		}
		
		return flag;
	}	
	private static boolean check_log(Element log) {
		//fantasma della check log
		return true;
	}	

	private static void stateChartGeneration(String pSCXMLModel, Element pDocumentRoot, File pInputFile) {
		try {
			String initialState = pDocumentRoot.getAttribute("initial").getValue();
			List<String> stateNames = Common.getStateNames(pDocumentRoot);
			List<String> variableNames = Common.getVariableNames(pDocumentRoot);
			List<String> eventNames = Common.getEventNames(pDocumentRoot);
			ASMCodeCreator.create(pSCXMLModel, stateNames, initialState);
			GUICodeCreator.create(pSCXMLModel, variableNames, eventNames);
			LauncherCodeCreator.create(pSCXMLModel);
			copyModelFile(pSCXMLModel, pInputFile);
			System.out.println("End generation for model '" + pSCXMLModel + "', run the Launcher in its package to execute the StateChart.\n");
			try {
				Thread.sleep(100); // 
				System.out.println(pSCXMLModel + "." + "Launcher");
//				Class<?> classe = Class.forName(pSCXMLModel + Conf.filesep + "Launcher");
				Class<?> classe = Class.forName(pSCXMLModel + "." + "Launcher");
				String[] params = null;
				Method metodo = classe.getMethod("main", String[].class);
				Method[] metodi = classe.getMethods();
				for (int i=0; i<metodi.length; i++)
		        {System.out.println(metodi[i].getName());
		        }	              
//				Object[] args = {};
//				metodo.invoke(null, args);
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (InvocationTargetException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Ha funzionato?\n");
		} catch (JDOMException a_JDOMException) {
			a_JDOMException.printStackTrace();
		} catch (IOException an_IOException) {
			an_IOException.printStackTrace();
		}
	}

	private static void copyModelFile(String pSCXMLModel, File pInputFile) {
		try {
			BufferedReader bufferedInput = new BufferedReader(new FileReader(pInputFile));
			BufferedWriter bufferedOutput = new BufferedWriter(new FileWriter(new File(Conf.source_dir + Conf.filesep + pSCXMLModel
					+ Conf.filesep + "model.scxml")));
			String inputLine = "";
			inputLine = bufferedInput.readLine();
			// copying the first line and writing the warning
			bufferedOutput.write(inputLine + Conf.linesep);
			bufferedOutput.write("<!--\n\t Never change this 'model' file!\n\t Modify the original one in Conf.data_dir.\n -->" + Conf.linesep);
			// copying the rest of the file
			inputLine = bufferedInput.readLine();
//			System.out.println("Letta linea " + inputLine);
			while (inputLine != null) {
//				System.out.println("Letta linea " + inputLine);
				bufferedOutput.write(inputLine + Conf.linesep);
				inputLine = bufferedInput.readLine();
			}
			bufferedInput.close();
			bufferedOutput.close();
		} catch (IOException an_IOException) {
			an_IOException.printStackTrace();
		}
	}

}