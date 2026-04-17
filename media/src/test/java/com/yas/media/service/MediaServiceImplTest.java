package com.yas.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.media.config.YasConfig;
import com.yas.media.mapper.MediaVmMapper;
import com.yas.media.model.Media;
import com.yas.media.model.dto.MediaDto;
import com.yas.media.repository.FileSystemRepository;
import com.yas.media.repository.MediaRepository;
import com.yas.media.viewmodel.MediaPostVm;
import com.yas.media.viewmodel.MediaVm;
import com.yas.media.viewmodel.NoFileMediaVm;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Spy
    private MediaVmMapper mediaVmMapper = Mappers.getMapper(MediaVmMapper.class);

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private FileSystemRepository fileSystemRepository;

    @Mock
    private YasConfig yasConfig;

    @InjectMocks
    private MediaServiceImpl mediaService;

    @Nested
    class GetFile {

        private Media media;

        @BeforeEach
        void setUp() {
            media = new Media();
            media.setId(1L);
            media.setFileName("test.png");
            media.setFilePath("/storage/test.png");
            media.setMediaType("image/png");
        }

        @Test
        void whenMediaExistsAndFileNameMatches_thenReturnFileContent() {
            InputStream fakeStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
            when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
            when(fileSystemRepository.getFile("/storage/test.png")).thenReturn(fakeStream);

            MediaDto result = mediaService.getFile(1L, "test.png");

            assertThat(result.getContent()).isNotNull();
            assertThat(result.getMediaType()).isEqualTo(org.springframework.http.MediaType.IMAGE_PNG);
        }

        @Test
        void whenMediaNotFound_thenReturnEmptyDto() {
            when(mediaRepository.findById(1L)).thenReturn(Optional.empty());

            MediaDto result = mediaService.getFile(1L, "test.png");

            assertThat(result.getContent()).isNull();
            assertThat(result.getMediaType()).isNull();
        }

        @Test
        void whenFileNameDoesNotMatch_thenReturnEmptyDto() {
            when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));

            MediaDto result = mediaService.getFile(1L, "wrong-name.png");

            assertThat(result.getContent()).isNull();
            assertThat(result.getMediaType()).isNull();
        }

        @Test
        void whenFileNameMatchesCaseInsensitive_thenReturnFileContent() {
            InputStream fakeStream = new ByteArrayInputStream(new byte[]{1});
            when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
            when(fileSystemRepository.getFile("/storage/test.png")).thenReturn(fakeStream);

            MediaDto result = mediaService.getFile(1L, "TEST.PNG");

            assertThat(result.getContent()).isNotNull();
        }
    }

    @Nested
    class GetMediaById {

        @Test
        void whenMediaExists_thenReturnMediaVmWithUrl() {
            NoFileMediaVm noFileMediaVm = new NoFileMediaVm(1L, "caption", "file.png", "image/png");
            when(mediaRepository.findByIdWithoutFileInReturn(1L)).thenReturn(noFileMediaVm);
            when(yasConfig.publicUrl()).thenReturn("http://localhost");

            MediaVm result = mediaService.getMediaById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUrl()).isEqualTo("http://localhost/medias/1/file/file.png");
        }

        @Test
        void whenMediaNotFound_thenReturnNull() {
            when(mediaRepository.findByIdWithoutFileInReturn(999L)).thenReturn(null);

            MediaVm result = mediaService.getMediaById(999L);

            assertThat(result).isNull();
        }
    }

    @Nested
    class RemoveMedia {

        @Test
        void whenMediaExists_thenDeleteSuccessfully() {
            NoFileMediaVm noFileMediaVm = new NoFileMediaVm(1L, "cap", "file.png", "image/png");
            when(mediaRepository.findByIdWithoutFileInReturn(1L)).thenReturn(noFileMediaVm);

            mediaService.removeMedia(1L);

            verify(mediaRepository).deleteById(1L);
        }

        @Test
        void whenMediaNotFound_thenThrowNotFoundException() {
            when(mediaRepository.findByIdWithoutFileInReturn(999L)).thenReturn(null);

            assertThatThrownBy(() -> mediaService.removeMedia(999L))
                .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class SaveMedia {

        @Test
        void whenFileNameOverrideProvided_thenUseTrimmedOverride() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "original.png", "image/png", new byte[]{1});
            MediaPostVm postVm = new MediaPostVm("caption", file, "  override.png  ");

            when(fileSystemRepository.persistFile(anyString(), any(byte[].class)))
                .thenReturn("/storage/override.png");
            when(mediaRepository.save(any(Media.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            Media result = mediaService.saveMedia(postVm);

            assertThat(result.getFileName()).isEqualTo("override.png");
        }

        @Test
        void whenFileNameOverrideNull_thenUseOriginalFilename() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "original.png", "image/png", new byte[]{1});
            MediaPostVm postVm = new MediaPostVm("caption", file, null);

            when(fileSystemRepository.persistFile(anyString(), any(byte[].class)))
                .thenReturn("/storage/original.png");
            when(mediaRepository.save(any(Media.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            Media result = mediaService.saveMedia(postVm);

            assertThat(result.getFileName()).isEqualTo("original.png");
        }

        @Test
        void whenSaving_thenSetsAllFieldsCorrectly() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpeg", "image/jpeg", new byte[]{1, 2});
            MediaPostVm postVm = new MediaPostVm("my caption", file, "renamed.jpeg");

            when(fileSystemRepository.persistFile(anyString(), any(byte[].class)))
                .thenReturn("/storage/renamed.jpeg");
            when(mediaRepository.save(any(Media.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            Media result = mediaService.saveMedia(postVm);

            assertThat(result.getCaption()).isEqualTo("my caption");
            assertThat(result.getMediaType()).isEqualTo("image/jpeg");
            assertThat(result.getFilePath()).isEqualTo("/storage/renamed.jpeg");
        }
    }

    @Nested
    class GetMediaByIds {

        @Test
        void whenIdsExist_thenReturnMediaVmsWithUrls() {
            Media m1 = new Media();
            m1.setId(1L);
            m1.setFileName("f1.png");
            Media m2 = new Media();
            m2.setId(2L);
            m2.setFileName("f2.png");

            when(mediaRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(m1, m2));
            when(yasConfig.publicUrl()).thenReturn("http://localhost");

            List<MediaVm> result = mediaService.getMediaByIds(List.of(1L, 2L));

            assertThat(result).hasSize(2);
            assertThat(result).allSatisfy(vm -> assertThat(vm.getUrl()).startsWith("http://localhost"));
        }

        @Test
        void whenNoIdsMatch_thenReturnEmptyList() {
            when(mediaRepository.findAllById(List.of(999L))).thenReturn(Collections.emptyList());

            List<MediaVm> result = mediaService.getMediaByIds(List.of(999L));

            assertThat(result).isEmpty();
        }
    }
}
