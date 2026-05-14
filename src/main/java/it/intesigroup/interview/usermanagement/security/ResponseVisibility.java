package it.intesigroup.interview.usermanagement.security;

// §2.2.3 – Regole di visibilità dei campi in base al ruolo del chiamante:
//   ADMIN    -> vede tutto (showTaxCode=true,  showRoles=true)
//   OPERATOR -> NON vede tax_code (showTaxCode=false, showRoles=true)
//   USER     -> NON vede tax_code né roles (showTaxCode=false, showRoles=false)
public record ResponseVisibility(boolean showTaxCode, boolean showRoles) {

    public static ResponseVisibility admin() {
        return new ResponseVisibility(true, true);
    }

    public static ResponseVisibility operator() {
        return new ResponseVisibility(false, true);
    }

    public static ResponseVisibility user() {
        return new ResponseVisibility(false, false);
    }
}
