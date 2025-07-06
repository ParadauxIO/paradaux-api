package io.paradaux.api.mappers;

import io.paradaux.api.models.ContactFormEntry;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ContactFormMapper {
    void insert(ContactFormEntry response);
    ContactFormEntry findById(int id);
    List<ContactFormEntry> findAll();
    void deleteById(int id);
}
