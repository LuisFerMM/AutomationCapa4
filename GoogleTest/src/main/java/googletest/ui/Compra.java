package googletest.ui;

import java.awt.AWTException;
import java.awt.Robot;
import java.io.File;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.bind.Validator;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import com.sun.glass.events.KeyEvent;

public class Compra {

	private static int valorCompra;

	public static void main(String[] args) throws ParseException {

//		Cargar la información de excel con una ventana

		WebDriver driver = null;

		GestorDeArchivos gda = new GestorDeArchivos();

		File cd = gda.getFileChromeDriver();
		File excel = gda.getFileExcel();

		if (cd != null && excel != null) {
			String rutaCD = cd.getAbsolutePath();
			String rutaExcel = excel.getAbsolutePath();
			ArrayList<Chip> chips = gda.convertirXlsxADatos();
			System.setProperty("webdriver.chrome.driver", rutaCD);
			driver = new ChromeDriver();

			driver.get("https://miportal.entel.cl/");
//		posible sleep
			boolean primeraVez = true;
//		Iteraciones mientras lee el excel
			for (int i = 0; i < chips.size(); i++) {

				try {
					String url = driver.getCurrentUrl();

					iniciarSesion(driver, url, chips.get(i));

					url = driver.getCurrentUrl();

//		Obtiene el saldo anterior a la compra y valida que no supere el saldo
					while (!existsElementByXPath(driver, "//*[@id=\"mobileAssetBalance\"]/div/div/div[1]/div/div/h2"))
						Thread.sleep(1000);
					String saldoString = driver
							.findElement(By.xpath("//*[@id=\"mobileAssetBalance\"]/div/div/div[1]/div/div/h2"))
							.getText();
					int saldoInt = obtenerValorEnInt(saldoString);
					if (primeraVez) {
						primeraVez = false;
						seleccionarBolsa(driver, url);
						while (!existsElementByXPath(driver, "//*[@id=\"Voz\"]/div/div[2]/div[2]"))
							Thread.sleep(600);
						String total = driver.findElement(By.xpath("//*[@id=\"Voz\"]/div/div[2]/div[2]")).getText();
						valorCompra = obtenerValorEnInt(total);
					}
					Date date = new Date();
					if (saldoInt < valorCompra) {
						cerrarSesion(driver);
						modificarDatosDe(chips.get(i), saldoInt, saldoInt, date);
					} else {
						if (driver.getCurrentUrl().equals(url))
							seleccionarBolsa(driver, url);
						int nuevoSaldo = terminarProcesoDeCompra(driver, saldoString, url);
						cerrarSesion(driver);
						modificarDatosDe(chips.get(i), saldoInt, nuevoSaldo, date);
					}
				} catch (InterruptedException e) {
					gda.generarExcel();
					JOptionPane.showMessageDialog(new JFrame(), "Error al cargar componentes");
				} catch (WebDriverException e2) {
					gda.generarExcel();
					JOptionPane.showMessageDialog(new JFrame(), "Caída inesperada de internet/servidor");
				}
			}
			gda.generarExcel();
		}
		System.exit(0);
		driver.close();
	}

	private static void modificarDatosDe(Chip chip, int saldoInt, int nuevoSaldo, Date fechaActual) {
		String seCompro = "";
		if (saldoInt == nuevoSaldo)
			seCompro = "Problema al comprar";
		chip.setSaldo(saldoInt, nuevoSaldo);
		DateFormat horaInicio = new SimpleDateFormat("HH:mm");
		DateFormat fechaInicio = new SimpleDateFormat("dd-MMM-aaaa");
		chip.setFechaInicio(fechaActual);
		chip.setHoraCompra(fechaActual);
		System.out.println(chip.getNumeroLista() + " " + chip.getNumeroSim() + " " + chip.getRut() + " "
				+ chip.getClave() + " " + nuevoSaldo + " " + horaInicio.format(fechaActual) + " "
				+ fechaInicio.format(fechaActual) + " " + seCompro);
	}

	private static void seleccionarBolsa(WebDriver driver, String url) throws InterruptedException {
		driver.findElement(By.xpath("//*[@id=\"mobileAssetBalance\"]/div/div/a[2]/span")).click();
		while (driver.getCurrentUrl().equals(url))
			Thread.sleep(600);
		driver.findElement(By.xpath("//*[@id=\"tab_prepago\"]/div/a")).click();
		driver.findElement(By.xpath("//*[@id=\"prepago\"]/div/div[1]/div[2]/div[1]/form/div/div[3]/div/input")).click();
		driver.findElement(By.xpath("//*[@id=\"prepago\"]/div/div[1]/div[2]/div[1]/form/div/div[1]/div/input")).click();
		Thread.sleep(600);
	}

	private static int obtenerValorEnInt(String saldoString) throws ParseException {
		int saldoInt;
		NumberFormat formatter = NumberFormat.getInstance();
		formatter.setParseIntegerOnly(true);
		if (saldoString.startsWith("$"))
			saldoInt = formatter.parse(saldoString.substring(1)).intValue();
		else
			saldoInt = Integer.parseInt(saldoString);
		return saldoInt;
	}

	private static void cerrarSesion(WebDriver driver) throws InterruptedException {
		if (driver.getCurrentUrl().equals("https://miportal.entel.cl/miEntel/mi-cuenta")) {
			driver.findElement(By.xpath("//*[@id=\"myAccountDropDownLink\"]/a/div")).click();
			Thread.sleep(400);
			driver.findElement(By.xpath("//*[@id=\"action-PROFILE_LOGOUT-1\"]/span")).click();
		} else {
			driver.findElement(By.xpath("//*[@id=\"header\"]/div/ul/li/a")).click();
			Thread.sleep(400);
			driver.findElement(By.id("action-PROFILE_LOGOUT")).click();
		}
		Thread.sleep(2000);
	}

	private static int terminarProcesoDeCompra(WebDriver driver, String saldoString, String url)
			throws InterruptedException, ParseException {
		driver.findElement(By.xpath("//*[@id=\"prepago\"]/div/div[2]/div[2]/div[2]/span[2]/input")).click();
		driver.findElement(By.xpath("//*[@id=\"prepago\"]/div/div[2]/div[2]/div[2]/span[2]/span[3]")).click();

//		Revisa si la compra se realizó exitosamente
		Thread.sleep(8000);
		driver.get("https://miportal.entel.cl/miEntel/mi-cuenta");
		int times = 4;
		while (!existsElementByXPath(driver, "//*[@id=\"mobileAssetBalance\"]/div/div/div[1]/div/div/h2"))
			Thread.sleep(2000);
		String saldoReciente = driver.findElement(By.xpath("//*[@id=\"mobileAssetBalance\"]/div/div/div[1]/div/div/h2"))
				.getText();
		while (driver.findElement(By.xpath("//*[@id=\"mobileAssetBalance\"]/div/div/div[1]/div/div/h2")).getText()
				.equals(saldoString) && times > 0) {
			driver.get("https://miportal.entel.cl/miEntel/mi-cuenta");
			Thread.sleep(2500);
			times--;
		}
		saldoReciente = driver.findElement(By.xpath("//*[@id=\"mobileAssetBalance\"]/div/div/div[1]/div/div/h2"))
				.getText();
		return obtenerValorEnInt(saldoReciente);
	}

	private static void iniciarSesion(WebDriver driver, String url, Chip entrada) throws InterruptedException {
		while (!existsElementByClass(driver, "text"))
			Thread.sleep(600);
		driver.findElement(By.className("text")).click();
		String subWin = null;
//		while(!existsElementByName(driver, "username"))
		Thread.sleep(800);
		driver.findElement(By.name("username")).sendKeys("" + entrada.getNumeroSim());
		driver.findElement(By.name("rutt")).sendKeys("" + entrada.getRut());
		driver.findElement(By.name("password")).sendKeys("" + entrada.getClave());
		Thread.sleep(400);
		driver.findElement(By.id("action-PROFILE_LOGIN")).click();
		int cont = 6;
		while (driver.getCurrentUrl().equals(url) && cont > 0) {
			Thread.sleep(2000);
			cont--;
		}
		driver.navigate().refresh();
		if (driver.getCurrentUrl().equals(url))
			Thread.sleep(2000);
	}

	private static boolean existsElementByClass(WebDriver driver, String className) {
		try {
			driver.findElement(By.className(className));
		} catch (NoSuchElementException e) {
			return false;
		}
		return true;
	}

	private static boolean existsElementByXPath(WebDriver driver, String xpath) {
		try {
			driver.findElement(By.xpath(xpath));
		} catch (NoSuchElementException e) {
			return false;
		}
		return true;
	}

	private static boolean existsElementByName(WebDriver driver, String name) {
		try {
			driver.findElement(By.name(name));
		} catch (NoSuchElementException e) {
			return false;
		}
		return true;
	}

	private static boolean existsElementById(WebDriver driver, String id) {
		try {
			driver.findElement(By.id(id));
		} catch (NoSuchElementException e) {
			return false;
		}
		return true;
	}
}
