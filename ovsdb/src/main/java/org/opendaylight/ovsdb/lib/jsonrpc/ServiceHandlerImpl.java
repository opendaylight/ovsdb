package org.opendaylight.ovsdb.lib.jsonrpc;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;


public class ServiceHandlerImpl<E> implements ServiceHandler {

    Class<? extends E> serviceInterface;
    Map<String, Handler> handlers = Maps.newHashMap();
    E receiver;
    ObjectMapper mapper = null;


    public ServiceHandlerImpl(Class<? extends E> serviceInterface, E reciever, ObjectMapper mapper) {
        this.serviceInterface = serviceInterface;
        this.receiver = reciever;
        this.mapper = mapper;
        init();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object> handleCall(String methodName, JsonNode paramsNode) {

        Handler invoker = handlers.get(methodName);
        if (null == invoker) {
            throw new RuntimeException(String.format("no invoker found for method %s", methodName));
        }
        try {
            Object invoke = invoker.invoke(paramsNode, receiver);
            List<Object> result = null;
            if (! List.class.isInstance(invoke)) {
                //note(ashwin): if the return type is list we blanket assumes that it is the result, but there may be
                // case where we are getting a list that has to be wrapped in the result array.
                result = Lists.newArrayList(invoke);
            } else {
                result = (List) invoke;
            }
            return result;

        } catch (InvocationTargetException e) {
            throw new RuntimeException("Unexpected", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unexpected", e);
        }
    }

    private void init() {
        for (Method method : serviceInterface.getMethods()) {
            Invokable<?, Object> from = Invokable.from(method);
            if (from.isAnnotationPresent(JsonRpcOperation.class)) {
                if (handlers.containsKey(method.getName())) {
                    throw new RuntimeException("cannot have more than one handler for same operation");
                }

                ImmutableList<Parameter> parameters = from.getParameters();
                JavaType[] parameterJTypes = new JavaType[parameters.size()];
                for (int i = 0; i < parameters.size(); i++) {
                    TypeToken<?> type = parameters.get(i).getType();
                    JavaType javaType = TypeFactory.defaultInstance().constructType(type.getType());
                    parameterJTypes[i] = javaType;
                }
                String name = method.getName();
                JsonRpcOperation annotation = from.getAnnotation(JsonRpcOperation.class);
                if (! JsonRpcOperation.DEFAULT_VALUE.equals(annotation.value())) {
                   name = annotation.value();
                }
                handlers.put(name, new Handler(from, parameterJTypes));
            }
        }

    }


    class Handler {
        Invokable invokable;
        JavaType[] parameterTypes;

        Handler(Invokable<?, Object> invokable, JavaType[] parameterTypes) {
            this.invokable = invokable;
            this.parameterTypes = parameterTypes;
        }

        @SuppressWarnings("unchecked")
        public Object invoke(JsonNode paramsNode, Object target)
                throws InvocationTargetException, IllegalAccessException {

            Object[] params = new Object[parameterTypes.length];

            if (parameterTypes.length == 0) {
                params[0] = mapper.convertValue(paramsNode, parameterTypes[0]);
            } else if (paramsNode.isArray() && paramsNode.size() == parameterTypes.length) {
                for (int i = 0; i < parameterTypes.length; i++) {
                    try {
                        params[i] = mapper.convertValue(paramsNode.get(i), parameterTypes[i]);
                    } catch (Exception e) {
                        throw new RuntimeException(String.format("Exception converting params at index %d to type %s",
                                i, parameterTypes[i].toString()));
                    }
                }

            } else {
                throw new UnsupportedOperationException("insufficient parameters");
            }

            return invokable.invoke(target, params);
        }
    }

}
