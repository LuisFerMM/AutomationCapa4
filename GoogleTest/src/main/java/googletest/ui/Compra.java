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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import com.sun.glass.events.KeyEvent;

public class Compra {

	private static int valorCompra;

	public static void main(String[] args) throws ParseException {

//		Cargar la información de excel con una ventana

		WebDriver driver;

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

				String url = driver.getCurrentUrl();

				try {
					iniciarSesion(driver, url, chips.get(i));

					url = driver.getCurrentUrl();

//		Obtiene el saldo anterior a la compra y valida que no supere el saldo
					String saldoString = driver
							.findElement(By.xpath("//*[@id=\"mobileAssetBalance\"]/div/div/div[1]/div/div/h2"))
							.getText();
					int saldoInt = obtenerValorEnInt(saldoString);
					if (primeraVez) {
						primeraVez = false;
						seleccionarBolsa(driver, url);
						String total = driver.findElement(By.xpath("//*[@id=\"Voz\"]/div/div[2]/div[2]")).getText();
						valorCompra = obtenerValorEnInt(total);
					}
					if (saldoInt < valorCompra) {
						cerrarSesion(driver);
					} else {
						if (driver.getCurrentUrl().equals(url))
							seleccionarBolsa(driver, url);
						int nuevoSaldo = terminarProcesoDeCompra(driver, saldoString, url);
						cerrarSesion(driver);
						 Date date = new Date();
						modificarDatosDe(chips.get(i), saldoInt, nuevoSaldo, date);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			gda.generarExcel();
		}
		System.exit(0);
//		driver.close();
	}

	private static void modificarDatosDe(Chip chip, int saldoInt, int nuevoSaldo, Date fechaActual) {
		String seCompro = "";
		if(saldoInt == nuevoSaldo) 
			seCompro = "Problema al comprar";
		DateFormat horaInicio = new SimpleDateFormat("HH:mm");
		DateFormat fechaInicio = new SimpleDateFormat("dd-MMM-aaaa");
		chip.setFechaInicio(fechaActual);
		chip.setHoraCompra(fechaActual);
		System.out.println(chip.getNumeroLista() + " " + chip.getNumeroSim() + " " + chip.getRut() + " " + chip.getClave() + " " + nuevoSaldo + " " + horaInicio.format(fechaActual) + " " + fechaInicio.format(fechaActual) + " " + seCompro);
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
		if(driver.getCurrentUrl().equals("https://miportal.entel.cl/miEntel/mi-cuenta")) {
			driver.findElement(By.xpath("//*[@id=\"myAccountDropDownLink\"]/a/div")).click();
			Thread.sleep(200);
			driver.findElement(By.xpath("//*[@id=\"action-PROFILE_LOGOUT-1\"]/span")).click();
		}
		else {driver.findElement(By.xpath("//*[@id=\"header\"]/div/ul/li/a")).click();
		Thread.sleep(200);
		driver.findElement(By.id("action-PROFILE_LOGOUT")).click();
		}
		Thread.sleep(2500);
	}

	private static int terminarProcesoDeCompra(WebDriver driver, String saldoString, String url)
			throws InterruptedException, ParseException {
		driver.findElement(By.xpath("//*[@id=\"prepago\"]/div/div[2]/div[2]/div[2]/span[2]/input")).click();
		driver.findElement(By.xpath("//*[@id=\"prepago\"]/div/div[2]/div[2]/div[2]/span[2]/span[3]")).click();
									
//		Revisa si la compra se realizó exitosamente
		Thread.sleep(8000);
		driver.get("https://miportal.entel.cl/miEntel/mi-cuenta");
		int times = 3;
		Thread.sleep(2000);
		String saldoReciente = driver.findElement(By.xpath("//*[@id=\"mobileAssetBalance\"]/div/div/div[1]/div/div/h2"))
				.getText();
		while (driver.findElement(By.xpath("//*[@id=\"mobileAssetBalance\"]/div/div/div[1]/div/div/h2"))
				.getText().equals(saldoString) && times > 0) {
			driver.get("https://miportal.entel.cl/miEntel/mi-cuenta");
			Thread.sleep(2000);
			times--;
		}
		saldoReciente = driver.findElement(By.xpath("//*[@id=\"mobileAssetBalance\"]/div/div/div[1]/div/div/h2"))
				.getText();
		return obtenerValorEnInt(saldoReciente);
	}

	private static void iniciarSesion(WebDriver driver, String url, Chip entrada) throws InterruptedException {
		driver.findElement(By.className("text")).click();
		String subWin = null;
		Thread.sleep(1000);
		driver.findElement(By.name("username")).sendKeys("" + entrada.getNumeroSim());
		driver.findElement(By.name("rutt")).sendKeys("" + entrada.getRut());
		driver.findElement(By.name("password")).sendKeys("" + entrada.getClave());
		driver.findElement(By.id("action-PROFILE_LOGIN")).click();
		while (driver.getCurrentUrl().equals(url))
			Thread.sleep(1000);
		driver.navigate().refresh();
		Thread.sleep(2600);
	}

}
