package com.yas.media.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.media.model.Media;
import com.yas.media.model.dto.MediaDto;
import com.yas.media.service.MediaService;
import com.yas.media.viewmodel.MediaVm;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MediaController.class)
@AutoConfigureMockMvc(addFilters = false)
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaService mediaService;

    @Test
    void create_whenValidRequest_thenReturn200() throws Exception {
        Media media = new Media();
        media.setId(1L);
        media.setCaption("test caption");
        media.setFileName("test.png");
        media.setMediaType("image/png");

        when(mediaService.saveMedia(any())).thenReturn(media);

        MockMultipartFile file = new MockMultipartFile(
            "multipartFile", "test.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/medias")
                .file(file)
                .param("caption", "test caption")
                .param("fileNameOverride", "test.png"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.caption").value("test caption"))
            .andExpect(jsonPath("$.fileName").value("test.png"));
    }

    @Test
    void get_whenMediaExists_thenReturn200() throws Exception {
        MediaVm mediaVm = new MediaVm(1L, "caption", "file.png", "image/png", "http://url/file.png");
        when(mediaService.getMediaById(1L)).thenReturn(mediaVm);

        mockMvc.perform(get("/medias/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.caption").value("caption"))
            .andExpect(jsonPath("$.url").value("http://url/file.png"));
    }

    @Test
    void get_whenMediaNotFound_thenReturn404() throws Exception {
        when(mediaService.getMediaById(999L)).thenReturn(null);

        mockMvc.perform(get("/medias/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_whenMediaExists_thenReturn204() throws Exception {
        doNothing().when(mediaService).removeMedia(1L);

        mockMvc.perform(delete("/medias/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void delete_whenMediaNotFound_thenThrowException() throws Exception {
        doThrow(new NotFoundException("Media %s is not found", 999L))
            .when(mediaService).removeMedia(999L);

        mockMvc.perform(delete("/medias/999"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void getByIds_whenMediasExist_thenReturn200() throws Exception {
        MediaVm mediaVm1 = new MediaVm(1L, "cap1", "f1.png", "image/png", "http://url/1");
        MediaVm mediaVm2 = new MediaVm(2L, "cap2", "f2.png", "image/png", "http://url/2");
        when(mediaService.getMediaByIds(List.of(1L, 2L))).thenReturn(List.of(mediaVm1, mediaVm2));

        mockMvc.perform(get("/medias").param("ids", "1", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void getByIds_whenNoMediasFound_thenReturn404() throws Exception {
        when(mediaService.getMediaByIds(List.of(999L))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/medias").param("ids", "999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getFile_whenFileExists_thenReturnFile() throws Exception {
        byte[] fileContent = new byte[]{1, 2, 3, 4};
        MediaDto mediaDto = MediaDto.builder()
            .content(new ByteArrayInputStream(fileContent))
            .mediaType(MediaType.IMAGE_PNG)
            .build();
        when(mediaService.getFile(eq(1L), eq("test.png"))).thenReturn(mediaDto);

        mockMvc.perform(get("/medias/1/file/test.png"))
            .andExpect(status().isOk());
    }
}
