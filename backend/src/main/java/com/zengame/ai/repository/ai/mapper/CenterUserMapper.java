package com.zengame.ai.repository.ai.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CenterUserMapper {

    List<String> selectActiveJobFamilies();
}
