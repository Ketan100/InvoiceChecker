package com.sanavi.app.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sanavi.app.config.EmailConfig;
import com.sanavi.app.model.InvDiscSts;
import com.sanavi.app.model.Invoice;
import com.sanavi.app.model.PO;
import com.sanavi.app.repo.PoRepository;
import com.sanavi.app.service.IInvDiscStsService;
import com.sanavi.app.service.IInvoiceService;

@Controller
@RequestMapping("/invoice")
public class InvoiceController 
{
	@Autowired
	private IInvoiceService service;
	@Autowired
	private PoRepository repo;
	@Autowired
	private EmailConfig email;
	@Autowired
	private IInvDiscStsService iservice;
	
	
	
	//get all data
	@GetMapping("invoicedata")
	public String InvoiceDataPage(ModelMap map)
	{
		List<Invoice> invList=service.findAll();
		map.addAttribute("invList", invList);
		return "InvoiceData";
	}

	// delete invoice data
	@GetMapping("/delete")
	public String delete(@RequestParam Long invId)
	{
		service.delete(invId);
		return "redirect:invoicedata";
	}

	//view selected invoice data
	@GetMapping("/view")
	public String view(@RequestParam Long invId,ModelMap map)
	{
		
		Invoice inv=service.findOne(invId);
		/*PO po=repo.findByPoNumber(inv.getPo().getPoNumber());
		
		System.out.println(inv.getPo().getPoNumber());
		
		if(inv.getPo().getPoNumber().equals(po.getPoNumber()))
		{
			System.out.println("Invoice PO number is not match with PO number");
		}*/
		
		map.addAttribute("inv", inv);
		return "InvoiceDataView";
	}

	
	
	List<String> errorList1 = new ArrayList<String>();
	String invmail=null;
	
	
	@GetMapping("checkinvoice")
	public String CheckInvoice(@RequestParam Long invId,ModelMap map)
	{
		List<String> errorList = new ArrayList<String>();
		
		//get all invoice data
		Invoice inv=service.findOne(invId);
		//get ponumber
		String poNumber=inv.getPo().getPoNumber();
		
		//get vendor email address
		invmail=inv.getVendor().getVenEmail();
		
		//to get all PO data related to PO number
		PO po=repo.findByPoNumber(poNumber);
		
		// check invoice data and PO data
		
		if(!inv.getPo().getPoNumber().equals(po.getPoNumber()))
		
			errorList.add("PO Number Is Not Match");
		
		if(!inv.getGstnNo().equals(po.getVendorGstnNo()))
			errorList.add("GSTN Number Is Not Match");
		
		if(!inv.getBalGstn().equals(po.getBalGstnNo()))
			errorList.add("Bal GSTN Number Is Not Match");
		
		if(!inv.getVendor().getVenCode().equals(po.getVendor().getVenCode()))
			errorList.add("Vendor Code Is Not Match");
		
		/*if(inv.getPoDate()!=po.getPoDate().toString())
		{
			errorList.add("PO Date Not Match");
		}*/
		
		//get gst data and calculate amount
		double totalprice=po.getTotalPrice();
		double sgst=po.getStateGST();
		double cgst=po.getCentralGST();
		
		double invsgst=(sgst/100)*totalprice;
		double invcgst=(cgst/100)*totalprice;
		System.out.println(invcgst);
		System.out.println(invsgst);
		if(inv.getSgst()!=invsgst)
			errorList.add("SGST is Wrong");
		
		if(inv.getCgst()!=invcgst)
			errorList.add("CGST is Wrong");
		
		//get invoice line total and calulate with gst amount
		
		double finalinvoicetotal=(inv.getLineTotal())+(invsgst)+(invcgst);
		
		if(inv.getTotal()!=finalinvoicetotal)
			errorList.add("Fianl Total Is Not Match, Your Final Total:"+inv.getTotal() +"   " +"Correct Total:"+finalinvoicetotal);
		
		if(!inv.getSacCode().equals(po.getSacNo()))
			errorList.add("SAC Code Not Match");
		
		//dispaly List
		
		
			StringBuilder b = new StringBuilder();
			for(String error : errorList)
			    b.append(error).append("\n");

			String errorString = b.toString();
			System.out.println(errorString);
		
			String venCode=inv.getVendor().getVenCode();
		
		if(errorList.isEmpty())
		{
			map.addAttribute("errorList", "NO DISCREPANCY FOUND IN INVOICE");
			
			
			InvDiscSts invds=new InvDiscSts(
					"INVOICE OK",poNumber,venCode,inv,"NO DISCREPANCY"
					);
			
			iservice.save(invds);
			
			
		}
		
		else
		{
			errorList1=errorList;
		map.addAttribute("errorList", errorList1);
		
		InvDiscSts invds=new InvDiscSts(
				"INVOICE ERROR",poNumber,venCode,inv,errorString
				);
		
		iservice.save(invds);
		
		
		}
		
		
		//errorList.clear();
		
		return "CheckInvoice";
	}
	
	
	// Send Discrepancy To Vendor Email
	
	@GetMapping("/sendmail")
	public String sendEmail(ModelMap map)
	{
		StringBuilder b = new StringBuilder();
		for(String error : errorList1)
		    b.append(error).append("\n");

		String ierror = b.toString();
		System.out.println(ierror);
		
		System.out.println(invmail);
		email.sendEmail(invmail, "Invoice Discrepancy List",ierror, null);
		errorList1.clear();
		
		
		return "redirect:invoicedata";
	}
	
	
	@GetMapping("/checkstatus")
	public String statusCheking(@RequestParam Long invId,ModelMap map)
	{
		
		InvDiscSts i=iservice.findOne(invId);
		
		
		List<String> st=new ArrayList<String>();
		
		st.add(i.getStatus());
		st.add(i.getDiscrepancy());
		
		
		StringBuilder b = new StringBuilder();
		for(String sts : st)
		    b.append(sts).append(",");

		String sts = b.toString();
		System.out.println(sts);
		
		
		map.addAttribute("invdts", sts);
		
		return "invoicestatus";
	}
	
	
	// logic for showing po and invoice data with discrepancy
	
	@GetMapping("showdisc")
	public String showDisc(@RequestParam Long invId,ModelMap map)
	{
		List<String> errorList = new ArrayList<String>();
		
		Invoice i=service.findOne(invId);
		String poNumber=i.getPo().getPoNumber();
		//to get all PO data related to PO number
		PO po=repo.findByPoNumber(poNumber);
		
		//get vendor email address
				invmail=i.getVendor().getVenEmail();
		
		//amount calculation
		
		//get gst data and calculate amount
				double totalprice=po.getTotalPrice();
				double sgst=po.getStateGST();
				double cgst=po.getCentralGST();
				
				double invsgst=(sgst/100)*totalprice;
				double invcgst=(cgst/100)*totalprice;
				System.out.println(invcgst);
				System.out.println(invsgst);
				
		//get invoice line total and calulate with gst amount
				
				double finalinvoicetotal=(i.getLineTotal())+(invsgst)+(invcgst);
				
				
		
		
		
		//invoice data
		map.addAttribute("po", poNumber);
		map.addAttribute("gst", i.getGstnNo());
		map.addAttribute("bgst", i.getBalGstn());
		map.addAttribute("ven", i.getVendor().getVenCode());
		map.addAttribute("cgst", i.getCgst());
		map.addAttribute("sgst", i.getSgst());
		map.addAttribute("total", i.getTotal());
		map.addAttribute("sac", i.getSacCode());
		
		
		//po data
		map.addAttribute("po1", po.getPoNumber());
		map.addAttribute("gst1", po.getVendorGstnNo());
		map.addAttribute("bgst1", po.getBalGstnNo());
		map.addAttribute("ven1", po.getVendor().getVenCode());
		map.addAttribute("cgst1", po.getCentralGST());
		map.addAttribute("sgst1", po.getStateGST());
		map.addAttribute("total1", po.getTotalPrice());
		map.addAttribute("sac1",po.getSacNo());
		//discrepancy
		if(!i.getPo().getPoNumber().equals(po.getPoNumber()))
		{
			map.addAttribute("pod", "PO Number Is Not Match");
			errorList.add("PO Number Is Not Match");
		
		}
		else
		{
			map.addAttribute("pod", "PO Number Match");
		}
			
		if(!i.getGstnNo().equals(po.getVendorGstnNo()))
		{
			map.addAttribute("gstd", "GSTN Number Is Not Match");
			errorList.add("GSTN Number Not Match");
		}
		else
		{
			map.addAttribute("gstd", "GSTN Number Match");
		}
		
		if(!i.getBalGstn().equals(po.getBalGstnNo()))
		{
			map.addAttribute("bgstd", "Bal GSTN Number Not Match");
			errorList.add("Bal GSTN Number Not Match");
		}
		else
		{
			map.addAttribute("bgstd", "Bal GSTN Number Match");
		}
		
		
		if(!i.getVendor().getVenCode().equals(po.getVendor().getVenCode()))
		{
			map.addAttribute("vend", "Vendor Code Is Not Match");
			
			errorList.add("Vendor Code Is Not Match");
		}
		else
		{
			map.addAttribute("vend", "Vendor Code Match");
		}
			
		
		if(i.getCgst()!=invcgst)
		{
			map.addAttribute("dcgst", "CGST Calculation is Wrong");
			errorList.add("CGST Calculation is Wrong");
		}
		else
		{
			map.addAttribute("dcgst", "CGST Calculation Match");
		}
		
		if(i.getSgst()!=invsgst)
		{
			map.addAttribute("dsgst", "SGST Calculation is Wrong");
			errorList.add("SGST Calculation is Wrong");
		}
		else
		{
			map.addAttribute("dsgst", "SGST Calculation Match");
		}
		
		
		if(i.getTotal()!=finalinvoicetotal)
		{
			map.addAttribute("dtotal","Fianl Total Is Not Match, Your Final Total:"+i.getTotal() +"   " +"Correct Total:"+finalinvoicetotal );
			errorList.add("Fianl Total Is Not Match, Your Final Total:"+i.getTotal() +"   " +"Correct Total:"+finalinvoicetotal);
		
		}
		else
		{
			map.addAttribute("dtotal","Fianl Total Match, Your Final Total:"+i.getTotal() +"   " +"Correct Total:"+finalinvoicetotal );
		}
		
		if(!i.getSacCode().equals(po.getSacNo()))
		{
			map.addAttribute("sacd", "SAC Code Not Match");
			errorList.add("SAC Code Not Match");
		}
		else
		{
			map.addAttribute("sacd", "SAC Code Match");
		}
			
		
		
		
		//**************************************************
		
		
		//dispaly List
		
		
		StringBuilder b = new StringBuilder();
		for(String error : errorList)
		    b.append(error).append("\n");

		String errorString = b.toString();
		System.out.println(errorString);
	
		String venCode=i.getVendor().getVenCode();
	
	if(errorList.isEmpty())
	{
		map.addAttribute("msg", "NO DISCREPANCY FOUND IN INVOICE");
		
		
		InvDiscSts invds=new InvDiscSts(
				"INVOICE OK",poNumber,venCode,i,"NO DISCREPANCY"
				);
		
		iservice.save(invds);
		
		
	}
	
	else
	{
		errorList1=errorList;
	//map.addAttribute("errorList", errorList1);
	
		map.addAttribute("msg", "DISCREPANCY FOUND IN INVOICE");
	InvDiscSts invds=new InvDiscSts(
			"INVOICE ERROR",poNumber,venCode,i,errorString
			);
	
	iservice.save(invds);
	
	
	}
	
	
	//errorList.clear();
		
		
		
		
		
		return "ShowDiscrepancy";
	
	}
	
	//doc file opening
	/*@GetMapping("open")
	public String docx()
	{
		File file = new File("C:\\Users\\Comp01\\Documents\\sanavi\\invoice.docx");
		try {
			//Open the file using Desktop class
			Desktop.getDesktop().open(file);
		}catch (IOException exception){
			exception.printStackTrace();
		}
		return "redirect:invoicedata";
	}*/
	
	
	//show invoice correction png file
	
	@GetMapping("correction")
	public String cor()
	{
		return "dcorrection";
	}
	
}
