package googletest.ui;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Chip {

	private int numeroLista;
	private int numeroSim;
	private String rut;
	private String clave;
	private int saldo;
	private Date fechaInicio;
	private Date horaCompra;
	private boolean fueComprado;
	private Date fechaVencimiento;
	private String postCompra;
	private int oportunidades;
	
	public Chip(int numeroLista, int numeroSim, String rut, String clave, int saldo, Date fechaInicio, Date horaCompra,
			Date fechaVencimiento) {
		super();
		fueComprado = false;
		postCompra = "";
		oportunidades = 2;
		this.numeroLista = numeroLista;
		this.numeroSim = numeroSim;
		this.setRut(rut);
		this.setClave(clave);
		this.saldo = saldo;
		this.fechaInicio = fechaInicio;
		this.horaCompra = horaCompra;
		this.fechaVencimiento = fechaVencimiento;
	}
	
	public String getPostCompra() {
		return postCompra;
	}
	
	public void setPostCompra(String postCompra) {
		this.postCompra = postCompra;
	}

	public int getNumeroLista() {
		return numeroLista;
	}

	public void setNumeroLista(int numeroLista) {
		this.numeroLista = numeroLista;
	}

	public int getNumeroSim() {
		return numeroSim;
	}

	public void setNumeroSim(int numeroSim) {
		this.numeroSim = numeroSim;
	}

	public String getAtributoPorNumero(int num) {
		String salida = "";
		DateFormat horaInicio = new SimpleDateFormat("HH:mm");
		DateFormat fechaInicio = new SimpleDateFormat("dd-MMM-yyyy");
		switch (num) {
		case GestorDeArchivos.FECHA_INICIO_BOLSA:
			if (this.fechaInicio != null)
				salida = fechaInicio.format(this.fechaInicio);
			break;
		case GestorDeArchivos.HORA_DE_COMPRA:
			if (this.fechaInicio != null)
				salida = horaInicio.format(this.fechaInicio);
			break;
		case GestorDeArchivos.N_LISTA:
			salida = numeroLista + "";
			break;
		case GestorDeArchivos.N_SIM:
			salida = numeroSim + "";
			break;
		case GestorDeArchivos.RUT:
			salida = rut;
			break;
		case GestorDeArchivos.CLAVE:
			salida = clave;
			break;
		case GestorDeArchivos.SALDO_FINAL:
			salida = saldo + "";
			break;
		case GestorDeArchivos.SE_COMPRO:
			salida = postCompra;
			break;
		}
		return salida;
	}

	public int getSaldo() {
		return saldo;
	}

	public void setSaldo(int saldoAnterior, int saldo) {
		this.saldo = saldo;
	}

	public Date getFechaInicio() {
		return fechaInicio;
	}

	public void setFechaInicio(Date fechaInicio) {
		this.fechaInicio = fechaInicio;
	}

	public Date getHoraCompra() {
		return horaCompra;
	}

	public void setHoraCompra(Date horaCompra) {
		this.horaCompra = horaCompra;
	}

	public Date getFechaVencimiento() {
		return fechaVencimiento;
	}

	public void setFechaVencimiento(Date fechaVencimiento) {
		this.fechaVencimiento = fechaVencimiento;
	}

	public String getRut() {
		return rut;
	}

	public void setRut(String rut) {
		this.rut = rut;
	}

	public String getClave() {
		return clave;
	}

	public void setClave(String clave) {
		this.clave = clave;
	}

	public boolean fueComprado() {
		return fueComprado;
	}

	public void setEstadoCompra(boolean fueComprado) {
		this.fueComprado = fueComprado;
	}

	public int getOportunidades() {
		return oportunidades;
	}

	public void pierdeOportunidad() {
		oportunidades--;
	}

}
