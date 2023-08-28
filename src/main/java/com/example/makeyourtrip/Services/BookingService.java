package com.example.makeyourtrip.Services;

import com.example.makeyourtrip.Models.Booking;
import com.example.makeyourtrip.Models.Routes;
import com.example.makeyourtrip.Models.Seat;
import com.example.makeyourtrip.Models.TicketEntity;
import com.example.makeyourtrip.Models.Transport;
import com.example.makeyourtrip.Models.User;
import com.example.makeyourtrip.Repositories.BookingRepository;
import com.example.makeyourtrip.Repositories.TransportRepository;
import com.example.makeyourtrip.Repositories.UserRepository;
import com.example.makeyourtrip.RequestDto.BookingRequest;
import com.example.makeyourtrip.RequestDto.GetAvailableSeatsDto;
import com.example.makeyourtrip.ResponseDtos.AvailableSeatResponseDto;
import com.example.makeyourtrip.Transformers.BookingTransformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TransportRepository transportRepository;

    @Autowired
    private UserRepository userRepository;

    public List<AvailableSeatResponseDto> getAvailableSeatsResponse(GetAvailableSeatsDto entryDto){

        List<Booking> doneBookings = bookingRepository.findBookings(entryDto.getJourneyDate(),entryDto.getTransportId());
        Set<String> bookedSeats = new TreeSet<>();
        for(Booking booking:doneBookings){
            String str = booking.getSeatNos(); //1E,2E,3B,4B
            String[] bookedSeatsInThatBooking = str.split(",");

            for(String seatNo : bookedSeatsInThatBooking){
                bookedSeats.add(seatNo);
            }
        }

        Transport transport = transportRepository.findById(entryDto.getTransportId()).get();
        List<Seat> seatList = transport.getSeatList();
        //Total seats - Booked Seats :
        List<AvailableSeatResponseDto> finalAvailableSeats = new ArrayList<>();
        for(Seat seat : seatList){

            if(bookedSeats.contains(seat.getSeatNo())){
                continue;
            }
            else{
                //We will be building that response object
                AvailableSeatResponseDto availableSeat  = AvailableSeatResponseDto.builder()
                        .seatPrice(seat.getPrice())
                        .seatType(seat.getSeatType())
                        .seatNo(seat.getSeatNo())
                        .build();

                finalAvailableSeats.add(availableSeat);
            }
        }
        return finalAvailableSeats;
    }


    public String makeABooking(BookingRequest bookingRequest){

        User userObj = userRepository.findById(bookingRequest.getUserId()).get();
        Transport transportObj = transportRepository.findById(bookingRequest.getTransportId()).get();

        Booking booking = BookingTransformers.convertRequestToEntity(bookingRequest);

        TicketEntity ticketEntity = createTicketEntity(transportObj,bookingRequest);


        //Set the FK
        booking.setTransport(transportObj);
        booking.setUser(userObj);
        booking.setTicketEntity(ticketEntity);



        //Setting the bidirectional mappings
        //Setting for ticket
        ticketEntity.setBooking(booking);

        //Setting the booking obj in the transport
        transportObj.getBookings().add(booking);

        //Setting the booking obj in the userObject
        userObj.getBookingList().add(booking);


        //Save kaise kroge figure out krna ////

        return null;

    }

    private TicketEntity createTicketEntity(Transport transport,BookingRequest bookingRequest){

        Integer totalPricePaid = findTotalPricePaid(transport,bookingRequest.getSeatNos());
        String routeDetails = getRouteDetails(transport);

        TicketEntity ticketEntity = TicketEntity.builder().allSeatNos(bookingRequest.getSeatNos())
                .journeyDate(bookingRequest.getJourneyDate())
                .startTime(transport.getStartTime())
                .routeDetails(routeDetails)
                .totalCostPaid(totalPricePaid)
                .build();

        return ticketEntity;
    }

    private String getRouteDetails(Transport transport){

        //"DELHI TO BANGALORE"
        Routes routes = transport.getRoute();
        String result = routes.getFromCity() + " TO "+routes.getToCity();
        return result;

    }

    private Integer findTotalPricePaid(Transport transport,String seatNos){

        List<Seat>seatList=transport.getSeatList();
        Integer totalPrice=0;
        String[]seats= seatNos.split(",");
        Set<String>set=new HashSet<>();
        for(String s:seats)set.add(s);


        //just Iterate over the Seats...

        for(Seat seat:seatList)
        {
            if(set.contains(seat.getSeatNo()))
            {
                totalPrice+=seat.getPrice();
            }
        }
        return totalPrice;
    }




}