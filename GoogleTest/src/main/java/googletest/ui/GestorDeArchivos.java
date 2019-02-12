package googletest.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class GestorDeArchivos {

	public static final int N_LISTA = 0;
	public static final int N_SIM = 1;
	public static final int RUT = 2;
	public static final int CLAVE = 3;
	public static final int SALDO_FINAL = 4;
	public static final int FECHA_INICIO_BOLSA = 5;
	public static final int HORA_DE_COMPRA = 6;
	public static final int FECHA_VENCE_BOLSA = 7;

	private File cd;
	private File excel;
	private ArrayList<Chip> chips;

	public File getFileChromeDriver() {
		JFileChooser selectorChromeDriver = new JFileChooser();
		selectorChromeDriver.setFileSelectionMode(JFileChooser.FILES_ONLY);
//		selectorChromeDriver.setCurrentDirectory(File);
		selectorChromeDriver.setDialogTitle("Elige el archivo chromedriver.exe");
		selectorChromeDriver.showDialog(new JFrame(), "Seleccionar Driver");
		return cd = selectorChromeDriver.getSelectedFile();
	}

	public File getFileExcel() {
		JFileChooser selectorExcel = new JFileChooser();
		selectorExcel.setFileSelectionMode(JFileChooser.FILES_ONLY);
		selectorExcel.setCurrentDirectory(cd);
		selectorExcel.setDialogTitle("Elige el archivo Excel de compras a realizar");
		selectorExcel.showDialog(new JFrame(), "¡Comprar!");
		return excel = selectorExcel.getSelectedFile();
	}

	public ArrayList convertirXlsxADatos() {
		try {
			FileInputStream file = new FileInputStream(excel);
			// leer archivo excel
			XSSFWorkbook worbook = new XSSFWorkbook(file);
			// obtener la hoja que se va leer
			XSSFSheet sheet = worbook.getSheetAt(0);
			// obtener todas las filas de la hoja excel
			Iterator<Row> rowIterator = sheet.iterator();
			Row row;
			chips = new ArrayList<Chip>();
			while (rowIterator.hasNext()) {
				row = rowIterator.next();
				Iterator<Cell> cellIterator = row.cellIterator();
				Cell cell;
				int cont = 0;
				int nLista = 0, nSim = 0, saldo = 0;
				String rut = "", clave = "";
				Date fechaInicio = null, horaInicio = null, fechaVence = null;
				while (cellIterator.hasNext()) {
					cell = cellIterator.next();
						if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
							DecimalFormat formato = new DecimalFormat("###");
							switch (cont) {
							case FECHA_INICIO_BOLSA:
								fechaInicio = cell.getDateCellValue();
								break;
							case FECHA_VENCE_BOLSA:
								fechaVence = cell.getDateCellValue();
								break;
							case HORA_DE_COMPRA:
								horaInicio = cell.getDateCellValue();
								break;
							case N_LISTA:
								nLista = Integer.parseInt(formato.format(cell.getNumericCellValue()));
								break;
							case N_SIM:
								nSim = Integer.parseInt(formato.format(cell.getNumericCellValue()));
								break;
							case RUT:
//								if (cell.getStringCellValue().contains("k") || cell.getStringCellValue().contains("-"))
//									rut = cell.getStringCellValue();
//								else
									rut = formato.format(cell.getNumericCellValue());
							case CLAVE:
//								clave = formato.format(cell.getStringCellValue());
								clave = formato.format(cell.getNumericCellValue());
								if (clave.length() < 4)
									clave = "0" + clave;
								break;
							case SALDO_FINAL:
								saldo = Integer.parseInt(formato.format(cell.getNumericCellValue()));
								break;
							}
						}
						cont++;
				}
				if (nSim != 0)
					chips.add(new Chip(nLista, nSim, rut, clave, saldo, fechaInicio, horaInicio, fechaVence));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return chips;
	}

	public void generarExcel() {
		String nombreArchivo = "Prueba.xlsx";
		String rutaArchivo = "C:\\Users\\Luis\\OneDrive - Universidad Icesi\\Capa4\\Soporte 2019 ene-feb"
				+ nombreArchivo;
		String hoja = "Hoja1";

		XSSFWorkbook libro = new XSSFWorkbook();
		XSSFSheet hoja1 = libro.createSheet(hoja);
		// cabecera de la hoja de excel
		String[] header = new String[] { "N°LISTA", "N° SIM", "RUT", "CLAVE", "SALDO FINAL", "FECHA INICIO BOLSA",
				"HORA DE COMPRA" };

		// contenido de la hoja de excel
		String[][] document = new String[chips.size()][7];
		for (int i = 0; i < chips.size(); i++) {
			for (int j = 0; j < 7; j++) {
				document[i][j] = chips.get(i).getAtributoPorNumero(j);
			}
		}

		// poner negrita a la cabecera
		CellStyle style = libro.createCellStyle();
		Font font = libro.createFont();
		font.setBold(true);
		style.setFont(font);

		// generar los datos para el documento
		for (int i = 0; i <= document.length; i++) {
			XSSFRow row = hoja1.createRow(i);// se crea las filas
			for (int j = 0; j < header.length; j++) {
				if (i == 0) {// para la cabecera
					XSSFCell cell = row.createCell(j);// se crea las celdas para la cabecera, junto con la posición
					cell.setCellStyle(style); // se añade el style crea anteriormente
					cell.setCellValue(header[j]);// se añade el contenido
				} else {// para el contenido
					XSSFCell cell = row.createCell(j);// se crea las celdas para la contenido, junto con la posición
					cell.setCellValue(document[i - 1][j]); // se añade el contenido
				}
			}
		}

		File file;
		file = new File(rutaArchivo);
		try {
			FileOutputStream fileOuS = new FileOutputStream(file);
			if (file.exists()) {// si el archivo existe se elimina
				file.delete();
				System.out.println("Archivo eliminado");
			}
			libro.write(fileOuS);
			fileOuS.flush();
			fileOuS.close();
			System.out.println("Archivo Creado");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
