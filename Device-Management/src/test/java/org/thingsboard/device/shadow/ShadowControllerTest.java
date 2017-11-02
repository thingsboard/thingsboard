
package org.thingsboard.device.shadow;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.thingsboard.device.shadow.dao.DeviceShadowDao;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ShadowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    DeviceShadowDao mockDataSource;

    @Test
    public void shadowControllerShouldReturnUpdateStatus() throws Exception {
        String json = "{\"deviceName\":\"abc\",\"tags\":\"temp\"}";
        //String response = "{\"status\":\"updated\"}";
        this.mockMvc.perform(post("/update/available/tags").contentType(
                MediaType.APPLICATION_JSON).content(json)).andExpect(
                status().isOk())
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("updated"));
    }

    @Test
    public void shadowControllerShouldReturnDuplicateErrorStatus() throws Exception {
        String json = "{\"deviceName\":\"abc\",\"tags\":\"temp\"}";
        this.mockMvc.perform(post("/update/available/tags").contentType(
                MediaType.APPLICATION_JSON).content(json)).andExpect(
                status().isOk())
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("Duplicate tag"));
    }

    @Test
    public void shadowControllerShouldReturnErrorStatus() throws Exception {
        String json = "{\"sdc\":SDCSD}";
        //String response = "{\"status\":\"updated\"}";
        String response = "Unexpected character (S) at position 7.";
        this.mockMvc.perform(post("/update/available/tags").contentType(
                MediaType.APPLICATION_JSON).content(json)).andExpect(
                status().isOk())
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(response));
    }

    @Test
    public void shadowControllerShouldReturnAvailableTags() throws Exception {
        mockDataSource.getAvailableTagsForDevice("abc") ;
        this.mockMvc.perform(get("/available/tags").contentType(
                MediaType.APPLICATION_JSON).param("deviceName","abc")).andExpect(
                status().isOk())
                .andDo(print()).andExpect(status().isOk());
    }

    @Test
    public void shadowControllerShouldReturnEmptyTagList() throws Exception {
        List<String> list = new ArrayList<>();
        this.mockMvc.perform(get("/available/tags").contentType(
                MediaType.APPLICATION_JSON).param("deviceName","Invalid")).andExpect(
                status().isOk())
                .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.tags").isEmpty());
    }

    @Test
    public void shadowControllerShouldReturnDesiredTagStatus() throws Exception {
        String json = "{\"deviceName\":\"abc\",\"tags\":[\"temp\"]}";
        this.mockMvc.perform(post("/desired/tags").contentType(
                MediaType.APPLICATION_JSON).content(json)).andExpect(
                status().isOk())
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("updated"));
    }

    @Test
    public void shadowControllerShouldReturnErrorInDesiredTagStatus() throws Exception {
        String json = "{\"deviceName\":\"abc\",\"tags\":[\"temp\",\"pressure\"]}";
        this.mockMvc.perform(post("/desired/tags").contentType(
                MediaType.APPLICATION_JSON).content(json)).andExpect(
                status().isOk())
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void shadowControllerShouldReturnReporedTagStatus() throws Exception {
        String json = "{\"abc\":[\"temp\"]}";
        this.mockMvc.perform(post("/reported/tags").contentType(
                MediaType.APPLICATION_JSON).content(json)).andExpect(
                status().isOk())
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("updated"));
    }

    @Test
    public void shadowControllerShouldReturnErrorInReportedTagStatus() throws Exception {
        String json = "{\"deviceName\":\"abc\",\"tags\":[\"temp]}";
        this.mockMvc.perform(post("/reported/tags").contentType(
                MediaType.APPLICATION_JSON).content(json)).andExpect(
                status().isOk())
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.error").exists());
    }

}


