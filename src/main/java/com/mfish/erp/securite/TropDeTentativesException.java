package com.mfish.erp.securite;

/**
 * Levee lorsque le nombre maximal de tentatives de connexion est depasse.
 */
public class TropDeTentativesException extends RuntimeException {

    public TropDeTentativesException() {
        super("Trop de tentatives de connexion, reessayez plus tard");
    }
}
