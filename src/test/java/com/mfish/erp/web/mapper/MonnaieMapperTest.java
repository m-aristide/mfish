package com.mfish.erp.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfish.erp.web.config.MonnaieProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MonnaieMapperTest {

    private final MonnaieMapper monnaieMapper = new MonnaieMapper(new MonnaieProperties("FCFA"));

    @Test
    @DisplayName("formate sans decimale avec la devise")
    void formate_sans_decimale_avec_devise() {
        assertThat(monnaieMapper.formater(1500)).isEqualTo("1 500 FCFA");
        assertThat(monnaieMapper.formater(0)).isEqualTo("0 FCFA");
        assertThat(monnaieMapper.formater(1_000_000)).isEqualTo("1 000 000 FCFA");
    }

    @Test
    @DisplayName("groupe les chiffres par 3")
    void groupe_les_chiffres_par_3() {
        assertThat(monnaieMapper.formater(12_345_678)).isEqualTo("12 345 678 FCFA");
    }
}
