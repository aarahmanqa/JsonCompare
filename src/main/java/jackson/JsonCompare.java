package jackson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTAutoFilter;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFilter;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFilterColumn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class JsonCompare {

	static XSSFWorkbook workbook;
	static XSSFSheet sheet;
	static int rowCounter = 0;
	static String excelFileName = null;
	/**
	 * This is used to reverse the result i.e. We do one round of validation with source and target and in another round of validation, we interchange the values.
	 */
	static boolean reverseResult = false;
	static ArrayList<String> comparedValues = new ArrayList<String>(); 
	public static void main(String...arg)throws Throwable{

		ZonedDateTime zdt = ZonedDateTime.now();
		File resultsFolder = new File("results");
		if(!resultsFolder.exists())
			resultsFolder.mkdirs();
		excelFileName = "Result_" + zdt.format(DateTimeFormatter.ofPattern("dd_MMMM_yyyy_HH_mm")) + ".xlsx";		
		workbook = new XSSFWorkbook();
		FileOutputStream fileOutputStream = new FileOutputStream("results/" + excelFileName);
		workbook.write(fileOutputStream);
		fileOutputStream.close();
		String strSourceFolder = "/Users/ahamedabdulrahman/Downloads/CDMS-25641/preprod";
		String strTargetFolder = "/Users/ahamedabdulrahman/Downloads/CDMS-25641/prod";
		File sourceFolder = new File(strSourceFolder);
		File targetFolder = new File(strTargetFolder);

		for(File sourceFile : sourceFolder.listFiles()) {			
			for(File targetFile : targetFolder.listFiles()) {
				String sourceFileName = sourceFile.getName().replaceAll("[^0-9a-zA-Z]", "");
				String targetFileName = targetFile.getName().replaceAll("[^0-9a-zA-Z]", "");
				if(sourceFileName.equalsIgnoreCase(targetFileName)
						&& sourceFileName.equalsIgnoreCase("DSStore") == false) {
					ArrayNode arrayNode1 = sortArrayNode(convertJsonToArrayNode(sourceFile.getAbsolutePath()));
					ArrayNode arrayNode2 = sortArrayNode(convertJsonToArrayNode(targetFile.getAbsolutePath()));

					rowCounter = 0;
					sheet = workbook.createSheet(sourceFileName);
					Row row = sheet.createRow(0);
					sheet.setColumnWidth(0, 14000);
					sheet.setColumnWidth(1, 12000);
					sheet.setColumnWidth(2, 12000);
					sheet.setColumnWidth(3, 14000);
					sheet.setColumnWidth(4, 4000);

					CellStyle style = workbook.createCellStyle();      
					style.setFillForegroundColor(IndexedColors.AQUA.getIndex());
					style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
					Cell cell0 = row.createCell(0);
					cell0.setCellValue("Source Key");					
					cell0.setCellStyle(style);
					Cell cell1 = row.createCell(1);
					cell1.setCellValue("Source Value");
					cell1.setCellStyle(style);
					Cell cell2 = row.createCell(2);
					cell2.setCellValue("Target Value");
					cell2.setCellStyle(style);
					Cell cell3 = row.createCell(3);
					cell3.setCellValue("Target Key");
					cell3.setCellStyle(style);
					Cell cell4 = row.createCell(4);
					cell4.setCellValue("Result");
					cell4.setCellStyle(style);
					workbook.write(new FileOutputStream("results/" + excelFileName));

					reverseResult = false;
					compareJson(null, null, arrayNode1, arrayNode2);
					reverseResult = true;
					compareJson(null, null, arrayNode2, arrayNode1);

					// Apply filter and hide other columns.
					CellRangeAddress cellRangeAddress = new CellRangeAddress(0, rowCounter, 0, 4);
					sheet.setAutoFilter(cellRangeAddress);
					CTAutoFilter autoFilter = sheet.getCTWorksheet().getAutoFilter();
					CTFilterColumn filterColumn = autoFilter.insertNewFilterColumn(0);
					filterColumn.setColId(4);
					CTFilter newFilter = filterColumn.addNewFilters().insertNewFilter(0);
					newFilter.setVal("Mismatch");

					XSSFRow r1;
					for(Row r : sheet) {
						for (Cell c : r) {
							if (c.getColumnIndex()==4 && !c.getStringCellValue().equals("Mismatch")) {
								r1=(XSSFRow) c.getRow();
								if (r1.getRowNum()!=0) { /* Ignore top row */
									/* Hide Row that does not meet Filter Criteria */
									r1.getCTRow().setHidden(true); 
								}
							}				
						}
					}

					fileOutputStream = new FileOutputStream("results/" + excelFileName);
					workbook.write(fileOutputStream);
					fileOutputStream.close();
				}
			}
		}
	ZonedDateTime zdt2 = ZonedDateTime.now();
	long diff = ChronoUnit.SECONDS.between(zdt, zdt2);
	System.out.println("Total time taken = " + diff + " seconds");
	}

	private static void setCellValue(String string) {
		// TODO Auto-generated method stub

	}

	public static String getJsonValue(String key, JsonNode jsonNode, String keyChain) {
		if(keyChain == null) {
			keyChain = "";
		}
		if(jsonNode == null) {
			return null;
		}
		else if(jsonNode.isArray()) {				
			ArrayNode arrayNode1 = sortArrayNode((ArrayNode)jsonNode);			
			for(int i=0; i<arrayNode1.size(); i++) {
				Iterator<Entry<String, JsonNode>> arrFields = arrayNode1.get(i).fields();					
				while(arrFields.hasNext()) {
					Entry<String, JsonNode> arrEntry = arrFields.next();
					String arrKey = arrEntry.getKey();
					String thisKeyChain = keyChain+"["+i+"]."+arrKey;
					JsonNode arrValue1 = arrEntry.getValue();
					if(key.contains(thisKeyChain))
						return getJsonValue(key, arrValue1,thisKeyChain);
				}
			}				
		}
		else if(jsonNode.isContainerNode()) {
			Iterator<Entry<String, JsonNode>> fields1 = jsonNode.fields();			
			while(fields1.hasNext()) {
				Entry<String, JsonNode> entry = fields1.next();
				String thisKey = entry.getKey();
				String thisKeyChain = "";
				if(keyChain.isBlank())
					thisKeyChain = thisKey;
				else
					thisKeyChain = keyChain + "." + thisKey;
				JsonNode value1 = entry.getValue();
				if(key.contains(thisKeyChain))
					return getJsonValue(key, value1, thisKeyChain);
			}
		}
		else if(jsonNode.isValueNode()){
			if(keyChain.equals(key))
				return jsonNode.asText();
		}
		return null;
	}

	public static void compareJson(String keyChain1, String keyChain2, JsonNode jsonNode1, JsonNode jsonNode2) throws Throwable {

		String thisKeyChain1 = "";
		String thisKeyChain2 = "";
		if(keyChain1 == null)
			keyChain1 = "";
		if(keyChain2 == null)
			keyChain2 = "";
		if(jsonNode1 == null || jsonNode2 == null) {
			String sourceValue = "<missing>";
			if(jsonNode1 != null) {
				sourceValue = new ObjectMapper().writeValueAsString(jsonNode1);
			}
			String targetValue = "<missing>";
			if(jsonNode2 != null)
				targetValue = new ObjectMapper().writeValueAsString(jsonNode2);			
			compareJsonValueAsText(keyChain1,keyChain2,sourceValue,targetValue);
		}
		else if(jsonNode1.isArray() && jsonNode2.isArray()) {				
			//ArrayNode arrayNode1 = sortArrayNode((ArrayNode)jsonNode1);
			//ArrayNode arrayNode2 = sortArrayNode((ArrayNode)jsonNode2);
			ArrayNode arrayNode1 = (ArrayNode)jsonNode1;
			ArrayNode arrayNode2 = (ArrayNode)jsonNode2;
			ArrayList<Integer> usedIndices = new ArrayList<Integer>();
			for(int i=0; i<arrayNode1.size(); i++) {

				String firstValue1 = new ObjectMapper().writeValueAsString(getFirstValueNode(arrayNode1.get(i)));
				String firstValue2 = null;
				int j = 0;
				for(j=0; j<arrayNode2.size(); j++) {
					if(usedIndices.contains(j))
						continue;
					String temp = new ObjectMapper().writeValueAsString(getFirstValueNode(arrayNode2.get(j)));;
					if(temp == null)
						continue;
					if(firstValue1.equalsIgnoreCase(temp)) {
						firstValue2 = temp;
						usedIndices.add(j);
						break;
					}
				}

				if(firstValue2 == null) {
					thisKeyChain1 = keyChain1+"["+i+"]";
					thisKeyChain2 = keyChain2+"["+i+"]";
					compareJson(thisKeyChain1,thisKeyChain2, arrayNode1.get(i),null);
					continue;
				}

				//Array has list of elements like this: ["Ahamed","Abdul","Rahman"]. This will has hasNext() as false.
				Iterator<Entry<String, JsonNode>> arrFields = arrayNode1.get(i).fields();
				if(arrFields.hasNext() == false) {
					for(i=0;i<arrayNode1.size();i++) {
						String arr1 = new ObjectMapper().writeValueAsString(arrayNode1.get(i));
						for(j=0;j<arrayNode2.size();j++) {
							String arr2 = new ObjectMapper().writeValueAsString(arrayNode2.get(j));
							if(arr1.equalsIgnoreCase(arr2)) {
								thisKeyChain1 = keyChain1+"["+i+"]";
								thisKeyChain2 = keyChain2+"["+j+"]";
								compareJson(thisKeyChain1,thisKeyChain2,arrayNode1.get(i), arrayNode2.get(j));
								break;
							}
						}

						if(j == arrayNode2.size()) {
							compareJson(thisKeyChain1,thisKeyChain2,arrayNode1.get(i), null);
						}
					}
				}
				while(arrFields.hasNext()) {
					Entry<String, JsonNode> arrEntry = arrFields.next();
					String arrKey = arrEntry.getKey();
					thisKeyChain1 = keyChain1+"["+i+"]."+arrKey;
					thisKeyChain2 = keyChain2+"["+j+"]."+arrKey;
					JsonNode arrValue1 = arrEntry.getValue();
					JsonNode arrValue2 = arrayNode2.get(j).get(arrKey);
					compareJson(thisKeyChain1, thisKeyChain2, arrValue1,arrValue2);

				}
			}				
		}
		else if(jsonNode1.isContainerNode() && jsonNode2.isContainerNode()) {
			Iterator<Entry<String, JsonNode>> fields1 = jsonNode1.fields();
			if(fields1.hasNext() == false) {
				compareJsonValueAsText(keyChain1, keyChain2, jsonNode1, jsonNode2);
			}
			while(fields1.hasNext()) {
				Entry<String, JsonNode> entry = fields1.next();
				String thisKey = entry.getKey();
				thisKeyChain1 = "";
				if(keyChain1.isBlank())
					thisKeyChain1 = thisKey;
				else
					thisKeyChain1 = keyChain1 + "." + thisKey;
				if(keyChain2.isBlank())
					thisKeyChain2 = thisKey;
				else
					thisKeyChain2 = keyChain2 + "." + thisKey;
				JsonNode value1 = entry.getValue();
				JsonNode value2 = jsonNode2.get(thisKey);
				compareJson(thisKeyChain1, thisKeyChain2, value1, value2);
			}
		}
		else if(jsonNode1.isValueNode() && jsonNode2.isValueNode()){
			compareJsonValueAsText(keyChain1, keyChain2, jsonNode1, jsonNode2);
		}
		else {
			String sourceValue = jsonNode1.asText();
			String targetValue = jsonNode2.asText();
			System.out.println("Not Matching key = " + keyChain1 + " : \n" + sourceValue + " \n" + targetValue);
		}
	}

	public static JsonNode getFirstValueNode(JsonNode jsonNode) {
		if(jsonNode == null) {
			return null;
		}
		else if(jsonNode.isArray()) {				
			ArrayNode arrayNode1 = sortArrayNode((ArrayNode)jsonNode);			
			for(int i=0; i<arrayNode1.size(); i++) {
				Iterator<Entry<String, JsonNode>> arrFields = arrayNode1.get(i).fields();
				if(arrFields.hasNext() == false) {
					if(arrayNode1.size() > 0)
						return getFirstValueNode(arrayNode1.get(0));
				}

				while(arrFields.hasNext()) {
					Entry<String, JsonNode> arrEntry = arrFields.next();
					String arrKey = arrEntry.getKey();
					JsonNode arrValue1 = arrEntry.getValue();
					return getFirstValueNode(arrValue1);
				}
			}				
		}
		else if(jsonNode.isContainerNode()) {
			Iterator<Entry<String, JsonNode>> fields1 = jsonNode.fields();			
			while(fields1.hasNext()) {
				Entry<String, JsonNode> entry = fields1.next();
				JsonNode value1 = entry.getValue();				
				return getFirstValueNode(value1);
			}
		}
		else if(jsonNode.isValueNode()){
			return jsonNode;
		}
		return null;
	}

	public static ArrayNode sortArrayNode(ArrayNode arrayNode) {
		ArrayList<String> arrayList = new ArrayList<String>();
		ArrayList<JsonNode> arrayListNode = new ArrayList<JsonNode>(); 
		for(int i=0; i<arrayNode.size(); i++) {
			arrayList.add(arrayNode.get(i).toString());
			arrayListNode.add(arrayNode.get(i));
		}

		for(int i=0; i<arrayList.size()-1; i++) {
			for(int j=i+1; j<arrayList.size(); j++) {
				if(arrayList.get(i).compareTo(arrayList.get(j)) > 0) {					
					Collections.swap(arrayList, i, j);				
					Collections.swap(arrayListNode, i, j);
				}
			}
		}

		ArrayNode resultArrayNode = new ObjectMapper().createArrayNode();
		for(int i=0; i<arrayListNode.size(); i++) {
			resultArrayNode.add(arrayListNode.get(i));
		}

		return resultArrayNode;
	}

	public static void compareJsonValueAsText(String key1, String key2, JsonNode value1, JsonNode value2) throws Throwable{
		String sourceValue = new ObjectMapper().writeValueAsString(value1);
		String targetValue = new ObjectMapper().writeValueAsString(value2);
		compareJsonValueAsText(key1,key2,sourceValue,targetValue);
	}

	public static void compareJsonValueAsText(String key1, String key2, String sourceValue, String targetValue) throws Throwable{		
		Row row = sheet.createRow(++rowCounter);
		if(sourceValue.length() > 32700)
			sourceValue = sourceValue.substring(0, 32700);
		if(targetValue.length() > 32700)
			targetValue = targetValue.substring(0, 32700);
		if(reverseResult == false) {
			comparedValues.add(key1 + "|" + sourceValue + "|" + key2 + "|" + targetValue);
			if(sourceValue.equals(targetValue) == true) {
				System.out.println("Matching key = " + key1 + " : " + key2 + "\n" + sourceValue + " \n" + targetValue);
				row.createCell(0).setCellValue(key1);
				row.createCell(1).setCellValue(sourceValue);
				row.createCell(2).setCellValue(targetValue);
				row.createCell(3).setCellValue(key2);
				row.createCell(4).setCellValue("Match");			
			}
			else {
				System.out.println("Not Matching key = " + key1 + " : " + key2 + "\n" + sourceValue + " \n" + targetValue);
				row.createCell(0).setCellValue(key1);
				row.createCell(1).setCellValue(sourceValue);
				row.createCell(2).setCellValue(targetValue);
				row.createCell(3).setCellValue(key2);
				row.createCell(4).setCellValue("Mismatch");
				CellStyle style = workbook.createCellStyle();      			
				style.setFillForegroundColor(IndexedColors.RED.getIndex());
				style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
				row.getCell(4).setCellStyle(style);			
			}
		}
		else { //reverseResult is true
			comparedValues.contains(key2 + "|" + targetValue + "|" + key1 + "|" + sourceValue);
			if(sourceValue.equals(targetValue) == true) {
				System.out.println("Matching key = " + key1 + " : " + key2 + "\n" + sourceValue + " \n" + targetValue);
				row.createCell(0).setCellValue(key2);
				row.createCell(1).setCellValue(targetValue);
				row.createCell(2).setCellValue(sourceValue);
				row.createCell(3).setCellValue(key1);
				row.createCell(4).setCellValue("Match");			
			}
			else {
				System.out.println("Not Matching key = " + key1 + " : " + key2 + "\n" + sourceValue + " \n" + targetValue);
				row.createCell(0).setCellValue(key2);
				row.createCell(1).setCellValue(targetValue);
				row.createCell(2).setCellValue(sourceValue);
				row.createCell(3).setCellValue(key1);
				row.createCell(4).setCellValue("Mismatch");
				CellStyle style = workbook.createCellStyle();      			
				style.setFillForegroundColor(IndexedColors.RED.getIndex());
				style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
				row.getCell(4).setCellStyle(style);			
			}
		}
		FileOutputStream fileOutputStream = new FileOutputStream("results/" + excelFileName);
		workbook.write(fileOutputStream);
		fileOutputStream.close();
	}

	public static ArrayList<String> convertJsonToList(String fileName) throws Throwable {
		File sourceFile = new File(fileName);
		FileInputStream fis = new FileInputStream(sourceFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		ArrayList<String> jsonList = new ArrayList<String>();
		while(true) {
			String readLine = br.readLine();
			if(readLine == null)
				break;
			jsonList.add(readLine);			
		}
		return jsonList;
	}

	public static ArrayNode convertJsonToArrayNode(String fileName) throws Throwable {
		File sourceFile = new File(fileName);
		FileInputStream fis = new FileInputStream(sourceFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		ArrayNode arrayNode = new ObjectMapper().createArrayNode();

		while(true) {
			String readLine = br.readLine();			
			if(readLine == null)
				break;
			arrayNode.add(new ObjectMapper().readValue(readLine, JsonNode.class));
		}		
		return arrayNode;
	}


}
