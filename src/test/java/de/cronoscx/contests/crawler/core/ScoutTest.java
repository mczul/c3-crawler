package de.cronoscx.contests.crawler.core;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Disabled
@DisplayName("Core :: Scout")
@DisplayNameGeneration(ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ScoutTest {
    @Mock
    private Assessor assessor;
    @InjectMocks
    private Scout underTest;

    @BeforeEach
    void beforeEach() {
        when(assessor.relevant(any(URI.class))).thenReturn(true);
    }

    @Nested
    class Parse {

        @ParameterizedTest
        @CsvSource(value = {
                "impressum      | http://my-site.com        | http://my-site.com/impressum",
                "/impressum     | http://my-site.com        | http://my-site.com/impressum",
                "/impressum     | http://my-site.com/       | http://my-site.com/impressum",
                "/impressum     | http://my-site.com/a      | http://my-site.com/impressum",
                "/impressum     | http://my-site.com/a/     | http://my-site.com/impressum",
                "/              | http://my-site.com/a/     | http://my-site.com/",
                "/              | http://my-site.com/a      | http://my-site.com/",

        }, delimiter = '|')
        void must_handle_content_without_base_href_settings(String href, URI source, URI expected) {
            // given

            // when
            final var actual = underTest.parse(href, source, Optional.empty());

            // then
            assertThat(actual).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource(value = {
                // full href
                "https://www.whatever.com/a/b.html | http://my-site.com/a      | /x  | https://www.whatever.com/a/b.html",
                // relative href
                "impressum                         | http://my-site.com        | /x  | http://my-site.com/x/impressum",
                // absolute href
                "/impressum                        | http://my-site.com        | /x  | http://my-site.com/impressum",
                "/impressum                        | http://my-site.com/       | /x  | http://my-site.com/impressum",
                "/impressum                        | http://my-site.com/a      | /x  | http://my-site.com/impressum",
                "/impressum                        | http://my-site.com/a/     | /x  | http://my-site.com/impressum",
                "/                                 | http://my-site.com/a/     | /x  | http://my-site.com/",
                "/                                 | http://my-site.com/a      | /x  | http://my-site.com/",

        }, delimiter = '|')
        void must_handle_content_with_base_href_settings(String href, URI source, URI baseHref, URI expected) {
            // given

            // when
            final var actual = underTest.parse(href, source, Optional.empty());

            // then
            assertThat(actual).isEqualTo(expected);
        }


    }

}