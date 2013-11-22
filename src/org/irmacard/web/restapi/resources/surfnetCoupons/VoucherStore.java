package org.irmacard.web.restapi.resources.surfnetCoupons;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class VoucherStore {
	private static VoucherStore vs;
	DataSource ds = null;
	
	private VoucherStore () throws VoucherException {
		try {
			Context initCtx = new InitialContext();
			Context envCtx = (Context) initCtx.lookup("java:comp/env");
			ds = (DataSource) envCtx.lookup("jdbc/irma_voucher");
		} catch (NamingException e) {
			e.printStackTrace();
			System.out.println("Cannot access database!");
			throw new VoucherException("Cannot access database!");
		}
	}

	public static VoucherStore getStore () throws VoucherException {
		if(vs == null) {
			vs = new VoucherStore();
		}
		
		return vs;
	}
	
	public String getVoucherCode(String user_id) throws VoucherException {
		String voucher = null;
		Connection conn = null;
		ResultSet result;

		try {
			conn = ds.getConnection();
			
			// Test if user_id already has a key
			PreparedStatement test_exists = conn
					.prepareStatement("SELECT * FROM vouchers WHERE user_id = ?");
			test_exists.setString(1, user_id);
			
			result = test_exists.executeQuery();
			if(result.next()) {
				voucher = result.getString("voucher_code");
				System.out.println("User already has voucher: " + voucher);
				return voucher;
			}
			result.close();
			
			// User has no given voucher, retrieving new one
			Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
	                   ResultSet.CONCUR_UPDATABLE);
			result = stmt.executeQuery("SELECT * FROM vouchers WHERE user_id is null");
			if(result.next()) {
				// Option for voucher code
				String possible_voucher = result.getString("voucher_code");
				System.out.println("Option for voucher: " + possible_voucher);
				
				// Store user id in database
				result.updateString("user_id", user_id);
				result.updateRow();
				result.close();
				
				// When no exceptions, return the voucher
				voucher = possible_voucher;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("SQL error");
		} finally {
			try { conn.close(); } catch (Exception e) {} 
		}
		
		return voucher;
	}
}
