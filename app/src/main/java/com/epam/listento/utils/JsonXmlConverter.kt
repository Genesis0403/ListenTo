package com.epam.listento.utils

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.lang.reflect.Type

class JsonXmlConverter : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        for (annotation in annotations) {
            return when (annotation.annotationClass) {
                Json::class -> {
                    GsonConverterFactory.create().responseBodyConverter(type, annotations, retrofit)
                }
                Xml::class -> {
                    SimpleXmlConverterFactory.createNonStrict().responseBodyConverter(type, annotations, retrofit)
                }
                else -> super.responseBodyConverter(type, annotations, retrofit)
            }
        }
        return null
    }
}