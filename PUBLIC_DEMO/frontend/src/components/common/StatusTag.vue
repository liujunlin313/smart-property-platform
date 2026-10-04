<script setup>import { displayEnum, statusTagType } from '../../constants/presentation.js'; const props=defineProps({ value:{type:[String,Number],default:''},label:{type:String,default:''} })</script>
<template><el-tag size="small" :type="statusTagType(value)" effect="light">{{ label || displayEnum(value) }}</el-tag></template>
